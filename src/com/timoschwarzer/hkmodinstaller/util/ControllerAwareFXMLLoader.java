package com.timoschwarzer.hkmodinstaller.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;
import java.net.URL;

/**
 * Created by timo on 17.03.16.
 */
public class ControllerAwareFXMLLoader<R> {
    public static class ControllerAwareFXMLLoaderResult<T> {
        private T controller;
        private Node node;

        public T getController() {
            return controller;
        }

        public Node getNode() {
            return node;
        }

        private ControllerAwareFXMLLoaderResult(Node node, T controller) {
            this.controller = controller;
            this.node = node;
        }
    }

    public ControllerAwareFXMLLoaderResult<R> load(URL location) throws IOException {
        FXMLLoader loader = new FXMLLoader(location);
        return new ControllerAwareFXMLLoaderResult<>(loader.load(), loader.getController());
    }
}
