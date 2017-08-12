package com.timoschwarzer.hkmodinstaller.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class FileUtil {
    public static void mkdirp(String dir) throws IOException {
        final File dirFile = new File(dir);
        if (!dirFile.exists()) {
            Files.createDirectory(dirFile.toPath());
        }
    }
}
