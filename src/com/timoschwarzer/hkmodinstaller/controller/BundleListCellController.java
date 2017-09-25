package com.timoschwarzer.hkmodinstaller.controller;

import com.timoschwarzer.hkmodinstaller.data.ModBundle;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Wrapper for UI elements of a particular BundleListCell in the application
 */
public class BundleListCellController {
    public ImageView modImageView;
    public Label modNameLabel;
    public Label modInfoLabel;
    public AnchorPane bundleCell;

    /**
     * Extracts properties of the ModBundle to define UI elements of the associated BundleListCell
     *
     * @param bundle the ModBundle used to define the properties of the BundleListCellController
     * @throws IOException for any failure of the ByteArrayInputStream
     */
    public void setModBundle(ModBundle bundle) throws IOException {
        modImageView.setImage(new Image(new ByteArrayInputStream(bundle.getImage())));
        modNameLabel.setText(bundle.getName());
        modInfoLabel.setText(bundle.getVersion() + " - " + "by " + bundle.getAuthor());
    }
}
