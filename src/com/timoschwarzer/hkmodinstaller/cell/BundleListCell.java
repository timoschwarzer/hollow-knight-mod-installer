package com.timoschwarzer.hkmodinstaller.cell;

import com.timoschwarzer.hkmodinstaller.controller.BundleListCellController;
import com.timoschwarzer.hkmodinstaller.data.ModBundle;
import com.timoschwarzer.hkmodinstaller.data.ModDatabase;
import com.timoschwarzer.hkmodinstaller.util.ControllerAwareFXMLLoader;
import com.timoschwarzer.hkmodinstaller.util.ControllerAwareFXMLLoader.ControllerAwareFXMLLoaderResult;
import javafx.scene.control.ListCell;

import java.io.IOException;
import java.net.URL;

/**
 * A custom ListCell to display ModBundles in the application
 */
public class BundleListCell extends ListCell<ModBundle> {

    /**
     * Provides UI details to the cell, for use by the Controller
     */
    ControllerAwareFXMLLoaderResult<BundleListCellController> ui;

    public BundleListCell() throws IOException {
        ui = (new ControllerAwareFXMLLoader<BundleListCellController>()).load(new URL(getClass().getResource("/com/timoschwarzer/hkmodinstaller/fx/bundle_list_cell.fxml").toExternalForm()));
    }

    @Override
    protected void updateItem(ModBundle item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            try {
                ModBundle currentBundle = ModDatabase.getInstance().getCurrentModBundle();
                if (currentBundle != null && currentBundle.getId().equals(item.getId())) {
                    getStyleClass().add("hk-selected");
                } else {
                    getStyleClass().remove("hk-selected");
                }

                ui.getController().setModBundle(item);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setGraphic(ui.getNode());
            setText(null);
        }
    }
}
