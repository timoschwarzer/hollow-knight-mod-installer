package com.timoschwarzer.hkmodinstaller.data;

import com.timoschwarzer.hkmodinstaller.util.MD5Util;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.security.provider.MD5;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModBundle {
    private String filename;
    private ZipFile zipFile;
    private String id;
    private byte[] image;
    private String name;
    private String version;
    private String author;
    private ArrayList<SpecialFile> specialFiles = new ArrayList<SpecialFile>();
    private HashMap<String, String> compatibleVersions = new HashMap<>();

    public ModBundle(String filename) throws IOException {
        this.filename = filename;
        zipFile = new ZipFile(filename);
        load();
    }

    private void load() throws IOException {
        ZipEntry manifest = zipFile.getEntry("mod.json");

        JSONObject manifestObject = new JSONObject(IOUtils.toString(zipFile.getInputStream(manifest)));

        this.id = manifestObject.getString("id");
        this.image = IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("icon.png")));
        this.name = manifestObject.getString("name");
        this.version = manifestObject.getString("version");
        this.author = manifestObject.getString("author");

        zipFile.close();

        this.specialFiles.clear();
        JSONArray specialFilesArray = manifestObject.getJSONArray("special_files");
        for (int i = 0; i < specialFilesArray.length(); i++) {
            JSONObject specialFileObject = specialFilesArray.getJSONObject(i);
            SpecialFile specialFile = new SpecialFile(
                    specialFileObject.getString("name"),
                    specialFileObject.getString("target"),
                    specialFileObject.getString("game_hash")
            );
            this.specialFiles.add(specialFile);
        }

        JSONArray compatibleVersionsArray = manifestObject.getJSONArray("compatible_versions");
        for (int i = 0; i < compatibleVersionsArray.length(); i++) {
            JSONObject compatibleVersionObject = compatibleVersionsArray.getJSONObject(i);
            this.compatibleVersions.put(compatibleVersionObject.getString("hash"), compatibleVersionObject.getString("name"));
        }
    }

    public String getFilename() {
        return filename;
    }

    public String getId() {
        return id;
    }

    public String getIdHash() throws NoSuchAlgorithmException {
        return MD5Util.hash(getId());
    }

    public byte[] getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public ArrayList<SpecialFile> getSpecialFiles() {
        return specialFiles;
    }

    public HashMap<String, String> getCompatibleVersions() {
        return compatibleVersions;
    }
}
