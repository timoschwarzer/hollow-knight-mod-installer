package com.timoschwarzer.hkmodinstaller.util;

import javafx.stage.Stage;

/**
 * A simple class used to cast the Controller as one
 * which simply contains a primary stage. This allows the
 * modification of the Controller's stage without respect
 * to its other properties.
 */
public abstract class PrimaryStageAware {
    private Stage primaryStage;

    /** @return the primary stage of the instance */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Set the primary stage of the instance to the one given
     * @param primaryStage to set as the instance stage
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}