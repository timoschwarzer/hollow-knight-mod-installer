package com.timoschwarzer.hkmodinstaller.data;

import com.timoschwarzer.hkmodinstaller.util.MD5Util;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
public class ModBundle {
    /** Used to ensure compatibility of the mod bundle with the installer */
    public static final int MODBUNDLE_VERSION = 2;

    private String filename;
    private ZipFile zipFile;
    private String id;
    private byte[] image;
    private String name;
    private String version;
    private String author;
    private ArrayList<SpecialFile> specialFiles = new ArrayList<>();

    /* The map of all game versions which are compatible with this ModBundle */
    private HashMap<String, String> gameVersions = new HashMap<>();

    public ModBundle(String filename) throws Exception {
        this.filename = filename;
        zipFile = new ZipFile(filename);
        load();
    }

    /** Ensures and loads all elements of the ModBundle from the filename given */
    private void load() throws Exception {
        // Pre-initialization and version validation
        ZipEntry manifest = zipFile.getEntry("mod.json");

        JSONObject manifestObject = new JSONObject(IOUtils.toString(zipFile.getInputStream(manifest), StandardCharsets.UTF_8));

        if (manifestObject.isNull("modbundle_version") || manifestObject.getInt("modbundle_version") != MODBUNDLE_VERSION) {
            throw new Exception("The mod bundle you are trying to import is not compatible with this version of the mod installer.");
        }

        // Instance initialization
        this.id = manifestObject.getString("id");
        this.image = IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("icon.png")));
        this.name = manifestObject.getString("name");
        this.version = manifestObject.getString("version");
        this.author = manifestObject.getString("author");

        zipFile.close();

        this.specialFiles.clear();

        // Load game version information
        JSONArray gameVersionsArray = manifestObject.getJSONArray("game_versions");
        for (int i = 0; i < gameVersionsArray.length(); i++) {
            JSONObject gameVersionObject = gameVersionsArray.getJSONObject(i);
            this.gameVersions.put(gameVersionObject.getString("hash"), gameVersionObject.getString("name"));

            // Load special files from game version information
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

    /** @return the filename of the ModBundle */
    public String getFilename() {
        return filename;
    }

    /** @return the id of the ModBundle */
    public String getId() {
        return id;
    }

    /** @return the MD5 hash of the ModBundle's ID string */
    public String getIdHash() throws NoSuchAlgorithmException {
        return MD5Util.hash(getId());
    }

    /** @return the icon associated with the ModBundle */
    public byte[] getImage() {
        return image;
    }

    /** @return the name of the ModBundle */
    public String getName() {
        return name;
    }

    /** @return the version number of the ModBundle */
    public String getVersion() {
        return version;
    }

    /** @return the author associated with the ModBundle */
    public String getAuthor() {
        return author;
    }

    /** @return the set of all special files in the ModBundle */
    public ArrayList<SpecialFile> getSpecialFiles() {
        return specialFiles;
    }

    /** @return the map of compatible game versions of the ModBundle */
    public HashMap<String, String> getGameVersions() {
        return gameVersions;
    }
}
