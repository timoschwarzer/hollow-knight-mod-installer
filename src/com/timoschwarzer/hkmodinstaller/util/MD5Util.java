package com.timoschwarzer.hkmodinstaller.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class MD5Util {
    public static String hash(String stringToHash) throws NoSuchAlgorithmException {
        java.security.MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] array = md.digest(stringToHash.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte anArray : array) {
            sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String hashFile(String path) throws NoSuchAlgorithmException, IOException {
        java.security.MessageDigest md = MessageDigest.getInstance("MD5");

        InputStream is = Files.newInputStream(Paths.get(path));
        DigestInputStream dis = new DigestInputStream(is, md);

        byte[] buffer = new byte[4096];
        while (dis.read(buffer) != -1);

        is.close();
        dis.close();

        byte[] array = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte anArray : array) {
            sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
}
