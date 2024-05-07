module ru.alex.javafxrealtimefacedetection {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires opencv;

    opens ru.alex.javafxrealtimefacedetection to javafx.fxml;
    exports ru.alex.javafxrealtimefacedetection;
}