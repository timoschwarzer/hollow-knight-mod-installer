package com.timoschwarzer.hkmodinstaller.util;

import javafx.stage.Stage;

public abstract class PrimaryStageAware {
    private Stage primaryStage;

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}