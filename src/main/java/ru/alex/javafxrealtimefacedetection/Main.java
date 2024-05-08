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

public class Main extends Application {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private CascadeClassifier frontalFaceCascade;
    private CascadeClassifier profileFaceCascade;
    private VideoCapture capture;
    private SerialPort comPort;

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

        //  Распознавание лиц в режиме реального времени
        frontalFaceCascade = new CascadeClassifier();
        frontalFaceCascade.load("./src/main/resources/haarcascades/haarcascade_frontalface_alt.xml");

        profileFaceCascade = new CascadeClassifier();
        profileFaceCascade.load("./src/main/resources/haarcascades/haarcascade_profileface.xml");

        // Настройка последовательного порта
        comPort = SerialPort.getCommPorts()[0];
        comPort.openPort();

        new AnimationTimer() {
            @Override
            public void handle(long l) {
                imageView.setImage(getCaptureWithFaceDetection());
            }
        }.start();
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
                // Отправка координаты X центра лица на Arduino
                int faceCenterX = rect.x + rect.width / 2;
                String message = Integer.toString(faceCenterX) + "\n";
                comPort.writeBytes(message.getBytes(), message.getBytes().length);
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
        // Закрытие порта при завершении программы
        comPort.closePort();
    }
}
