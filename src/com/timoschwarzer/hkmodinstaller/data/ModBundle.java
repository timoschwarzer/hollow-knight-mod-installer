package com.timoschwarzer.hkmodinstaller.data;

import com.timoschwarzer.hkmodinstaller.util.MD5Util;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModBundle {
    public static final int MODBUNDLE_VERSION = 2;

    private String filename;
    private ZipFile zipFile;
    private String id;
    private byte[] image;
    private String name;
    private String version;
    private String author;
    private ArrayList<SpecialFile> specialFiles = new ArrayList<SpecialFile>();
    private HashMap<String, String> gameVersions = new HashMap<>();

    public ModBundle(String filename) throws Exception {
        this.filename = filename;
        zipFile = new ZipFile(filename);
        load();
    }

    private void load() throws Exception {
        ZipEntry manifest = zipFile.getEntry("mod.json");

        JSONObject manifestObject = new JSONObject(IOUtils.toString(zipFile.getInputStream(manifest)));

        if (manifestObject.isNull("modbundle_version") || manifestObject.getInt("modbundle_version") != MODBUNDLE_VERSION) {
            throw new Exception("The mod bundle you are trying to import is not compatible with this version of the mod installer.");
        }

        this.id = manifestObject.getString("id");
        this.image = IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("icon.png")));
        this.name = manifestObject.getString("name");
        this.version = manifestObject.getString("version");
        this.author = manifestObject.getString("author");

        zipFile.close();

        this.specialFiles.clear();

        JSONArray gameVersionsArray = manifestObject.getJSONArray("game_versions");
        for (int i = 0; i < gameVersionsArray.length(); i++) {
            JSONObject gameVersionObject = gameVersionsArray.getJSONObject(i);
            this.gameVersions.put(gameVersionObject.getString("hash"), gameVersionObject.getString("name"));

            JSONArray specialFilesArray = gameVersionObject.getJSONArray("special_files");
            for (int j = 0; j < specialFilesArray.length(); j++) {
                JSONObject specialFileObject = specialFilesArray.getJSONObject(j);
                SpecialFile specialFile = new SpecialFile(
                        specialFileObject.getString("name"),
                        specialFileObject.getString("target"),
                        gameVersionObject.getString("hash")
                );
                this.specialFiles.add(specialFile);
            }
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

    public HashMap<String, String> getGameVersions() {
        return gameVersions;
    }
}
