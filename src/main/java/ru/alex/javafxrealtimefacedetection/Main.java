package ru.alex.javafxrealtimefacedetection;

import com.fazecast.jSerialComm.SerialPort;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.*;


public class Main extends Application {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private CascadeClassifier frontalFaceCascade;
    private CascadeClassifier profileFaceCascade;
    private VideoCapture capture;
    private SerialPort comPort;
    private ScheduledExecutorService executorService;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        //  Доступ к камере с помощью OpenCV
        capture = new VideoCapture(0);
        ImageView imageView = new ImageView();
        HBox hbox = new HBox(imageView);
        Scene scene = new Scene(hbox);
        stage.setScene(scene);
        stage.show();
        // Создание периодической задачи для проверки доступных портов
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::checkAndOpenPort, 0, 1, TimeUnit.SECONDS);
        //  Распознавание лиц в режиме реального времени
        frontalFaceCascade = new CascadeClassifier();
        frontalFaceCascade.load("./src/main/resources/haarcascades/haarcascade_frontalface_alt.xml");

        profileFaceCascade = new CascadeClassifier();
        profileFaceCascade.load("./src/main/resources/haarcascades/haarcascade_profileface.xml");

        // Настройка последовательного порта, если доступны порты
        if (SerialPort.getCommPorts().length > 0) {
            comPort = SerialPort.getCommPorts()[0];
            comPort.openPort();
        }

        new AnimationTimer() {
            @Override
            public void handle(long l) {
                imageView.setImage(getCaptureWithFaceDetection());
            }
        }.start();
    }
    private void checkAndOpenPort() {
        if (comPort == null || !comPort.isOpen()) {
            SerialPort[] commPorts = SerialPort.getCommPorts();
            if (commPorts.length > 0) {
                comPort = commPorts[0];
                comPort.openPort();
            }
        }
    }
    public Image mat2Img(Mat mat) {
        MatOfByte bytes = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, bytes);
        InputStream inputStream = new ByteArrayInputStream(bytes.toArray());
        return new Image(inputStream);
    }

    public Image getCaptureWithFaceDetection() {
        Mat mat = new Mat();
        capture.read(mat);
        // Отражение изображения по горизонтали
        Core.flip(mat, mat, 1);
        Mat haarClassifiedImg = detectFace(mat);
        return mat2Img(haarClassifiedImg);
    }


    public Mat detectFace(Mat inputFrame) {
        MatOfRect frontalFaces = new MatOfRect();
        frontalFaceCascade.detectMultiScale(inputFrame, frontalFaces);

        if (frontalFaces.toArray().length > 0) {
            for (Rect rect : frontalFaces.toArray()) {
                Imgproc.rectangle(inputFrame, rect.tl(), rect.br(), new Scalar(0, 0, 255), 3);
                // Отправка координаты X центра лица на Arduino, если порт открыт
                if (comPort != null && comPort.isOpen()) {
                    int faceCenterX = rect.x + rect.width / 2;
                    String message = Integer.toString(faceCenterX) + "\n";
                    comPort.writeBytes(message.getBytes(), message.getBytes().length);
                }
            }
        } else {
            MatOfRect profileFaces = new MatOfRect();
            profileFaceCascade.detectMultiScale(inputFrame, profileFaces);
            for (Rect rect : profileFaces.toArray()) {
                Imgproc.rectangle(inputFrame, rect.tl(), rect.br(), new Scalar(0, 0, 255), 3);
            }
        }

        return inputFrame;
    }


    @Override
    public void stop() {
        // Закрытие порта и остановка задачи при завершении программы
        if (comPort != null) {
            comPort.closePort();
        }
        executorService.shutdown();
    }
}
