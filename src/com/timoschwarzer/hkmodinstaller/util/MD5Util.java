package com.timoschwarzer.hkmodinstaller.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A simple wrapper class for MD5 hash algorithm methods
 */
public abstract class MD5Util {

    /**
     * Performs an MD5 hash on the given string
     *
     * @param stringToHash
     * @return the successfully digested string
     * @throws NoSuchAlgorithmException for problems with retrieving the MD5 instance
     */
    public static String hash(String stringToHash) throws NoSuchAlgorithmException {
        java.security.MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] array = md.digest(stringToHash.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte anArray : array) {
            sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    /**
     * Performs an MD5 hash on the contents of the file at the given path
     *
     * @param path to the file to be digested
     * @return the successfully digested file
     * @throws NoSuchAlgorithmException for problems with retrieving the MD5 instance
     * @throws IOException for problems with file streaming
     */
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
