package com.timoschwarzer.hkmodinstaller.data;

import com.timoschwarzer.hkmodinstaller.util.FileUtil;
import com.timoschwarzer.hkmodinstaller.util.MD5Util;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModDatabase {
    private static ModDatabase instance = null;

    public static ModDatabase getInstance() throws Exception {
        if (instance == null) {
            instance = new ModDatabase();
        }
        return instance;
    }

    private HashMap<String, ModBundle> loadedBundles = new HashMap<>();
    private final String dataDirectory;
    private final String bundlesDirectory;
    private final String originalDirectory;
    private JSONObject configObject;

    private ModDatabase() throws Exception {
        this.dataDirectory = System.getProperty("user.home") + "/.hkmods";
        this.bundlesDirectory = this.dataDirectory + "/bundles";
        this.originalDirectory = this.dataDirectory + "/original";

        final File configFile = new File(this.dataDirectory + "/config.json");
        if (configFile.exists()) {
            configObject = new JSONObject(FileUtils.readFileToString(configFile));
        } else {
            configObject = new JSONObject();
            configObject.put("current_bundle", JSONObject.NULL);
            configObject.put("game_version", JSONObject.NULL);
            configObject.put("mod_hash", JSONObject.NULL);
            saveConfig();
        }

        FileUtil.mkdirp(this.dataDirectory);
        FileUtil.mkdirp(this.bundlesDirectory);
        FileUtil.mkdirp(this.originalDirectory);

        initializeBundles();

        if (getOriginalAssembly() != null) {
            String currentGameVersion = MD5Util.hashFile(getOriginalAssembly());
            if ((getCurrentModBundle() == null && !currentGameVersion.equals(configObject.getString("game_version"))) ||
                    (getCurrentModBundle() != null && !currentGameVersion.equals(configObject.getString("mod_hash")))) {
                configObject.put("game_version", currentGameVersion);
                configObject.put("current_bundle", JSONObject.NULL);
                saveConfig();
            }
        }
    }

    private void initializeBundles() throws Exception {
        loadedBundles.clear();
        final File[] bundleFiles = new File(this.bundlesDirectory).listFiles((dir, name) -> name.endsWith(".modbundle"));
        assert bundleFiles != null;

        String currentBundleId = configObject.isNull("current_bundle") ? null : configObject.getString("current_bundle");
        boolean currentBundleAvailable = currentBundleId == null;

        for (File bundleFile : bundleFiles) {
            ModBundle bundle = new ModBundle(bundleFile.getAbsolutePath());
            loadedBundles.put(bundle.getId(), bundle);

            if (!currentBundleAvailable && currentBundleId.equals(bundle.getId())) {
                currentBundleAvailable = true;
            }
        }

        if (!currentBundleAvailable) {
            configObject.put("current_bundle", JSONObject.NULL);
            saveConfig();
        }
    }

    public void importBundle(ModBundle bundle) throws Exception {
        Files.copy(new File(bundle.getFilename()).toPath(), new File(bundlesDirectory + "/" + bundle.getIdHash() + ".modbundle").toPath(), StandardCopyOption.REPLACE_EXISTING);
        initializeBundles();
    }

    public void deleteBundle(ModBundle bundle) throws Exception {
        if (getCurrentModBundle() != null && getCurrentModBundle().getId().equals(bundle.getId())) {
            unloadCurrentModBundle();
        }

        Files.delete(new File(bundlesDirectory + "/" + bundle.getIdHash() + ".modbundle").toPath());
        initializeBundles();
    }

    private void saveConfig() throws IOException {
        FileUtils.write(new File(this.dataDirectory + "/config.json"), configObject.toString());
    }

    public void unloadCurrentModBundle() throws IOException {
        ModBundle bundle = getCurrentModBundle();

        if (bundle != null) {
            final String gameDirectory = new File(Paths.get(new File(getOriginalAssembly()).getParent(), "..").toString()).getCanonicalPath();
            System.out.println("Game directory is: " + gameDirectory);

            ZipFile zipFile = new ZipFile(bundle.getFilename());

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.getName().startsWith("files/")) {
                    String relativePath = entry.getName().substring(6);

                    File targetFile = new File(Paths.get(gameDirectory, relativePath).toString());

                    if (targetFile.exists() && !targetFile.isDirectory()) {

                        // Restore original files and delete additional files
                        if (new File(Paths.get(originalDirectory, relativePath).toString()).exists()) {
                            Files.move(Paths.get(originalDirectory, relativePath), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Restored: " + targetFile.toString());
                        } else {
                            targetFile.delete();
                            System.out.println("Deleted:  " + targetFile.toString());
                        }
                    }

                }
            }

            // Restore special files
            for (SpecialFile specialFile : bundle.getSpecialFiles()) {
                if (specialFile.getTargetGameHash().equals(configObject.getString("game_version"))) {
                    File targetFile = new File(Paths.get(gameDirectory, specialFile.getTargetFileName()).toString());

                    if (targetFile.exists() && !targetFile.isDirectory()) {

                        // Restore original files and delete additional files
                        if (new File(Paths.get(originalDirectory, specialFile.getTargetFileName()).toString()).exists()) {
                            Files.move(Paths.get(originalDirectory, specialFile.getTargetFileName()), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Restored: " + targetFile.toString());
                        } else {
                            targetFile.delete();
                            System.out.println("Deleted:  " + targetFile.toString());
                        }
                    }
                }
            }

            zipFile.close();

            configObject.put("current_bundle", JSONObject.NULL);
            saveConfig();
        }
    }

    public boolean isCompatible(String id) {
        ModBundle bundle = loadedBundles.get(id);

        if (bundle != null) {
            return bundle.getGameVersions().keySet().contains(configObject.getString("game_version")) ||
                    bundle.getGameVersions().keySet().contains("any");
        } else {
            return false;
        }
    }

    public void loadModBundle(String id) throws IOException, NoSuchAlgorithmException {
        ModBundle bundle = loadedBundles.get(id);

        if (bundle != null) {
            unloadCurrentModBundle();

            final String gameDirectory = new File(Paths.get(new File(getOriginalAssembly()).getParent(), "..").toString()).getCanonicalPath();
            System.out.println("Game directory is: " + gameDirectory);

            ZipFile zipFile = new ZipFile(bundle.getFilename());

            // Extract default files
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.getName().startsWith("files/")) {
                    String relativePath = entry.getName().substring(6);

                    File targetFile = new File(Paths.get(gameDirectory, relativePath).toString());

                    boolean backupCreated = false;

                    if (targetFile.isDirectory()) {
                        FileUtil.mkdirp(targetFile.getAbsolutePath());
                    } else if (targetFile.exists()) {
                        File backupFile = new File(Paths.get(originalDirectory, relativePath).toString());
                        FileUtil.mkdirp(backupFile.getParent());

                        if (!(new File(Paths.get(originalDirectory, relativePath).toString()).exists())) {
                            Files.copy(targetFile.toPath(), Paths.get(originalDirectory, relativePath), StandardCopyOption.REPLACE_EXISTING);
                            backupCreated = true;
                        }

                        Files.copy(zipFile.getInputStream(entry), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    System.out.println("Extracted: " + targetFile.toString() + (backupCreated ? " [backed up]" : ""));
                }
            }

            // Extract special files
            for (SpecialFile specialFile : bundle.getSpecialFiles()) {
                if (specialFile.getTargetGameHash().equals(configObject.getString("game_version"))) {
                    File targetFile = new File(Paths.get(gameDirectory, specialFile.getTargetFileName()).toString());

                    boolean backupCreated = false;

                    if (targetFile.exists()) {
                        File backupFile = new File(Paths.get(originalDirectory, specialFile.getTargetFileName()).toString());
                        FileUtil.mkdirp(backupFile.getParent());

                        if (!(new File(Paths.get(originalDirectory, specialFile.getTargetFileName()).toString()).exists())) {
                            Files.copy(targetFile.toPath(), Paths.get(originalDirectory, specialFile.getTargetFileName()), StandardCopyOption.REPLACE_EXISTING);
                            backupCreated = true;
                        }

                        Files.copy(zipFile.getInputStream(zipFile.getEntry("special_files/" + specialFile.getFileName())), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    System.out.println("Extracted conditionally: " + specialFile.getFileName() + targetFile.toString() + (backupCreated ? " [backed up]" : ""));
                }
            }

            zipFile.close();

            String currentModHash = MD5Util.hashFile(getOriginalAssembly());

            configObject.put("current_bundle", bundle.getId());
            configObject.put("mod_hash", currentModHash);
            saveConfig();
        }
    }

    public Collection<ModBundle> getModBundles() {
        return loadedBundles.values();
    }

    public ModBundle getModBundle(String id) {
        return loadedBundles.get(id);
    }

    public ModBundle getCurrentModBundle() {
        return configObject.isNull("current_bundle") ? null : getModBundle(configObject.getString("current_bundle"));
    }

    public void setOriginalAssembly(String path) throws IOException, NoSuchAlgorithmException {
        configObject.put("original_assembly", path);
        configObject.put("game_version", MD5Util.hashFile(path));
        FileUtils.cleanDirectory(new File(originalDirectory));
        saveConfig();
    }

    public String getOriginalAssembly() {
        return configObject.isNull("original_assembly") ? null : configObject.getString("original_assembly");
    }

    public String getGameVersion() {
        return configObject.isNull("game_version") ? "---" : configObject.getString("game_version");
    }
}
