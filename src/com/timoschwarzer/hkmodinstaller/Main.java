package com.timoschwarzer.hkmodinstaller;

import com.timoschwarzer.hkmodinstaller.util.PrimaryStageAware;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * The class responsible for initializing the controller
 * and executing the functions of the mod loader.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fx/main.fxml"));
        loader.setControllerFactory((Class<?> type) -> {
            try {
                Object controller = type.newInstance();
                if (controller instanceof PrimaryStageAware) {
                    ((PrimaryStageAware) controller).setPrimaryStage(primaryStage);
                }
                return controller;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        primaryStage.getIcons().add(new Image(getClass().getResource("/com/timoschwarzer/hkmodinstaller/res/icon.ico").toExternalForm()));
        primaryStage.getIcons().add(new Image(getClass().getResource("/com/timoschwarzer/hkmodinstaller/res/icon.png").toExternalForm()));
        primaryStage.setTitle("Hollow Knight Mod Installer v0.3");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        Scene scene = new Scene(loader.load(), 600, 500);
        scene.getStylesheets().add(getClass().getResource("/com/timoschwarzer/hkmodinstaller/res/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
