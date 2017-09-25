package com.timoschwarzer.hkmodinstaller.controller;

import com.timoschwarzer.hkmodinstaller.cell.BundleListCell;
import com.timoschwarzer.hkmodinstaller.util.PrimaryStageAware;
import com.timoschwarzer.hkmodinstaller.data.ModBundle;
import com.timoschwarzer.hkmodinstaller.data.ModDatabase;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * The container for application elements and initialization.
 *
 */
public class Controller extends PrimaryStageAware implements Initializable {
    public Button loadButton;
    public TextField gameLocationTextField;
    public ImageView currentModImageView;
    public Label currentModNameLabel;
    public Label currentModInfoLabel;
    public ListView<ModBundle> modBundlesListView;
    public Button unloadCurrentButton;
    public Button deleteButton;
    public Label gameVersionLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            modBundlesListView.setCellFactory(param -> {
                try {
                    return new BundleListCell();
                } catch (IOException e) {
                    return null;
                }
            });

            modBundlesListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ModBundle>) c -> {
                boolean enable = modBundlesListView.getSelectionModel().getSelectedItems().size() > 0;
                deleteButton.setDisable(!enable);
                loadButton.setDisable(!enable);
            });

            while (ModDatabase.getInstance().getOriginalAssembly() == null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setResizable(true);
                alert.getDialogPane().setPrefSize(480, 300);
                alert.setTitle(getPrimaryStage().getTitle());
                alert.setHeaderText("Select the game assembly");
                alert.setContentText("Press OK to select your current game assembly (Assembly-CSharp.dll).\n\nWARNING:\nMake sure you are selecting an unmodded game assembly.");
                alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/timoschwarzer/hkmodinstaller/res/style.css").toExternalForm());
                alert.showAndWait();
                onChangeGameLocationButtonClick(null);
            }

            updateLoadedBundles();
            updateCurrentModBundle();
            updateGameInfo();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Couldn't load mod database:\n" + e.getMessage());
        }
    }

    public void onImportButtonClick(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import mod...");
        fileChooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Mod bundle", "*.modbundle"));
        File file = fileChooser.showOpenDialog(getPrimaryStage());

        if (file != null) {
            try {
                ModBundle bundle = new ModBundle(file.getAbsolutePath());
                ModDatabase.getInstance().importBundle(bundle);
                updateLoadedBundles();
            } catch (Exception e) {
                showErrorAlert("Error", "Couldn't load mod bundle:\n" + e.getMessage());
            }
        }
    }

    public void onDeleteButtonClick(ActionEvent actionEvent) throws Exception {
        ModDatabase.getInstance().deleteBundle(modBundlesListView.getSelectionModel().getSelectedItem());
        updateLoadedBundles();
        updateCurrentModBundle();
    }

    public void onLoadButtonClick(ActionEvent actionEvent) throws Exception {
        ModBundle bundle = modBundlesListView.getSelectionModel().getSelectedItems().get(0);
        boolean isCompatible = ModDatabase.getInstance().isCompatible(bundle.getId());

        if (!isCompatible) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            alert.getButtonTypes().setAll(noButton, yesButton);
            alert.setTitle(getPrimaryStage().getTitle());
            alert.setHeaderText("Warning");
            alert.setResizable(true);
            alert.getDialogPane().setPrefSize(480, 300);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/timoschwarzer/hkmodinstaller/res/style.css").toExternalForm());

            StringBuilder versions = new StringBuilder();
            for (String version : bundle.getGameVersions().values()) {
                versions.append(version).append("\n");
            }

            alert.setContentText("You are about to load a mod that is probably not compatible with your current game version.\nDo you want to continue?\n\nCompatible versions are:\n" + versions);

            if (alert.showAndWait().get() == yesButton) {
                // Hacky
                isCompatible = true;
            }
        }

        if (isCompatible) {

            try {
                if (ModDatabase.getInstance().canLoadAndUnload(modBundlesListView.getSelectionModel().getSelectedItems().get(0).getId())) {
                    ModDatabase.getInstance().loadModBundle(modBundlesListView.getSelectionModel().getSelectedItems().get(0).getId());
                } else {
                    showErrorAlert("Error", "Can't modify files!\nMake sure your game isn't running or try running with administrative privileges.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Error", "Error while loading mod bundle.\nSee the log for more information.");
            }

            updateLoadedBundles();
            updateCurrentModBundle();
        }
    }

    public void onUnloadCurrentButtonClick(ActionEvent actionEvent) throws Exception {
        try {
            if (ModDatabase.getInstance().canLoadAndUnload(ModDatabase.getInstance().getCurrentModBundle().getId())) {
                ModDatabase.getInstance().unloadCurrentModBundle();
            } else {
                showErrorAlert("Error", "Can't modify files!\nMake sure your game isn't running or try running with administrative privileges.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Error while unloading mod bundle.\nSee the log for more information.");
        }

        updateLoadedBundles();
        updateCurrentModBundle();
    }

    public void onChangeGameLocationButtonClick(ActionEvent actionEvent) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select game assembly");
        fileChooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Assembly-CSharp.dll", "Assembly-CSharp.dll"));
        File file = fileChooser.showOpenDialog(getPrimaryStage());

        if (file != null) {
            ModDatabase.getInstance().setOriginalAssembly(file.getAbsolutePath());
            updateGameInfo();
        }
    }

    private void updateLoadedBundles() throws Exception {
        modBundlesListView.getItems().setAll(ModDatabase.getInstance().getModBundles());
    }

    private void updateCurrentModBundle() throws Exception {
        ModBundle currentModBundle = ModDatabase.getInstance().getCurrentModBundle();
        if (currentModBundle == null) {
            currentModImageView.setImage(new Image(getClass().getResource("/com/timoschwarzer/hkmodinstaller/res/unmodded.jpg").toExternalForm()));
            currentModNameLabel.setText("Unmodded");
            currentModNameLabel.setStyle("-fx-font-style: italic");
            currentModInfoLabel.setText("Import a mod bundle and load it");
            unloadCurrentButton.setDisable(true);
        } else {
            currentModImageView.setImage(new Image(new ByteArrayInputStream(currentModBundle.getImage())));
            currentModNameLabel.setText(currentModBundle.getName());
            currentModNameLabel.setStyle("");
            currentModInfoLabel.setText("Version " + currentModBundle.getVersion() + "\n" + "by " + currentModBundle.getAuthor());
            unloadCurrentButton.setDisable(false);
        }
    }

    private void updateGameInfo() throws Exception {
        gameLocationTextField.setText(ModDatabase.getInstance().getOriginalAssembly());
        gameVersionLabel.setText("Game version: " + ModDatabase.getInstance().getGameVersion());
    }

    private void showErrorAlert(String header, String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(480, 300);
        alert.setTitle(getPrimaryStage().getTitle());
        alert.setHeaderText(header);
        alert.setContentText(text);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/timoschwarzer/hkmodinstaller/res/style.css").toExternalForm());
        alert.showAndWait();
    }
}
