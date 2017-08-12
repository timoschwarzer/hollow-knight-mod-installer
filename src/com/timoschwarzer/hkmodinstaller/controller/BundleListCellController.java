package com.timoschwarzer.hkmodinstaller.controller;

import com.timoschwarzer.hkmodinstaller.data.ModBundle;
import com.timoschwarzer.hkmodinstaller.data.ModDatabase;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class BundleListCellController {
    public ImageView modImageView;
    public Label modNameLabel;
    public Label modInfoLabel;
    public AnchorPane bundleCell;

    public void setModBundle(ModBundle bundle) throws IOException {
        modImageView.setImage(new Image(new ByteArrayInputStream(bundle.getImage())));
        modNameLabel.setText(bundle.getName());
        modInfoLabel.setText(bundle.getVersion() + " - " + "by " + bundle.getAuthor());
    }
}
