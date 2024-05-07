package ru.alex.javafxrealtimefacedetection;

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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // 7. Доступ к камере с помощью OpenCV
        capture = new VideoCapture(0);
        ImageView imageView = new ImageView();
        HBox hbox = new HBox(imageView);
        Scene scene = new Scene(hbox);
        stage.setScene(scene);
        stage.show();

        // 8. Распознавание лиц в режиме реального времени
        frontalFaceCascade = new CascadeClassifier();
        frontalFaceCascade.load("./src/main/resources/haarcascades/haarcascade_frontalface_alt.xml");

        profileFaceCascade = new CascadeClassifier();
        profileFaceCascade.load("./src/main/resources/haarcascades/haarcascade_profileface.xml");

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
        Mat haarClassifiedImg = detectFace(mat);
        return mat2Img(haarClassifiedImg);
    }

    public Mat detectFace(Mat inputFrame) {
        MatOfRect frontalFaces = new MatOfRect();
        frontalFaceCascade.detectMultiScale(inputFrame, frontalFaces);

        if (frontalFaces.toArray().length > 0) {
            for (Rect rect : frontalFaces.toArray()) {
                Imgproc.rectangle(inputFrame, rect.tl(), rect.br(), new Scalar(0, 0, 255), 3);
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

}
