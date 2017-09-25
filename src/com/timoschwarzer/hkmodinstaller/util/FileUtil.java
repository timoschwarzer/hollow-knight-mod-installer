package com.timoschwarzer.hkmodinstaller.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A simple wrapper class for file reading methods
 */
public abstract class FileUtil {

    /**
     * Atomically ensures the existence of a file and
     * creates a directory to its abstract path otherwise
     * @param dir the directory to be created
     * @throws IOException for problems creating a file or path from the given parameters
     */
    public static void mkdirp(String dir) throws IOException {
        final File dirFile = new File(dir);
        if (!dirFile.exists()) {
            Files.createDirectory(dirFile.toPath());
        }
    }
}
