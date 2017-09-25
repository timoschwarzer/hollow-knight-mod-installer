package com.timoschwarzer.hkmodinstaller.data;

import com.timoschwarzer.hkmodinstaller.util.FileUtil;
import com.timoschwarzer.hkmodinstaller.util.MD5Util;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    /**
     * A singular static instance of ModDatabase decreases
     * overhead and streamlines implementation in the Controller.
     * It also prevents concurrency issues which might arise from
     * misuse.
     *
     * @return the sole instance of the ModDatabase class
     */
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

        // Load or create config file
        final File configFile = new File(this.dataDirectory + "/config.json");
        if (configFile.exists()) {
            configObject = new JSONObject(FileUtils.readFileToString(configFile, StandardCharsets.UTF_8));
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

    /**
     * Initialize or reinitialize all ModBundles at instantiation or upon update
     *
     * @throws Exception for any failure in iteration over bundleFiles
     */
    private void initializeBundles() throws Exception {
        loadedBundles.clear();
        final File[] bundleFiles = new File(this.bundlesDirectory).listFiles((dir, name) -> name.endsWith(".modbundle"));
        assert bundleFiles != null;

        String currentBundleId = configObject.isNull("current_bundle") ? null : configObject.getString("current_bundle");
        boolean currentBundleAvailable = currentBundleId == null;

        for (File bundleFile : bundleFiles) {
            try {
                ModBundle bundle = new ModBundle(bundleFile.getAbsolutePath());
                loadedBundles.put(bundle.getId(), bundle);

                if (!currentBundleAvailable && currentBundleId.equals(bundle.getId())) {
                    currentBundleAvailable = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                bundleFile.delete();
            }
        }

        if (!currentBundleAvailable) {
            configObject.put("current_bundle", JSONObject.NULL);
            saveConfig();
        }
    }

    /**
     * Adds and initializes the given ModBundle
     *
     * @param bundle the ModBundle
     * @throws Exception for IOException in Files.copy() or Exception in initializeBundles()
     */
    public void importBundle(ModBundle bundle) throws Exception {
        Files.copy(new File(bundle.getFilename()).toPath(), new File(bundlesDirectory + "/" + bundle.getIdHash() + ".modbundle").toPath(), StandardCopyOption.REPLACE_EXISTING);
        initializeBundles();
    }

    /**
     * Unloads the given ModBundle if it matches the current one.
     *
     * @param bundle the ModBundle in question to be deleted
     * @throws Exception for IOException in unloadCurrentModBundle() or Exception in initializeBundles()
     */
    public void deleteBundle(ModBundle bundle) throws Exception {
        if (getCurrentModBundle() != null && getCurrentModBundle().getId().equals(bundle.getId())) {
            unloadCurrentModBundle();
        }

        Files.delete(new File(bundlesDirectory + "/" + bundle.getIdHash() + ".modbundle").toPath());
        initializeBundles();
    }


    /** Save the ModDatabase's current config */
    private void saveConfig() throws IOException {
        FileUtils.writeStringToFile(new File(this.dataDirectory + "/config.json"), configObject.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Iterates over the contents of a bundle, given by its ID, to
     * determine whether each ZipEntry may be written by the database.
     *
     * @param id of the bundle to be tested
     * @return whether the bundle with the given id is loadable and unloadable
     * @throws IOException for any failure in reading or writing the file
     */
    public boolean canLoadAndUnload(String id) throws IOException {
        ModBundle bundle = loadedBundles.get(id);

        if (bundle != null) {
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

                    if (!targetFile.canWrite()) {
                        System.out.println("Can't write to " + targetFile.getAbsolutePath());
                        return false;
                    }
                }
            }

            // Extract special files
            for (SpecialFile specialFile : bundle.getSpecialFiles()) {
                if (specialFile.getTargetGameHash().equals(configObject.getString("game_version"))) {
                    File targetFile = new File(Paths.get(gameDirectory, specialFile.getTargetFileName()).toString());

                    if (!targetFile.canWrite()) {
                        System.out.println("Can't write to " + targetFile.getAbsolutePath());
                        return false;
                    }
                }
            }

            zipFile.close();

            return true;
        } else {
            return false;
        }
    }

    /**
     * Commits all entries of the current mod bundle to the game directory.
     *
     * @throws IOException for any failure in reading or writing the file
     */
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
                        targetFile.delete();

                        if (new File(Paths.get(originalDirectory, relativePath).toString()).exists()) {
                            FileUtils.moveFile(new File(Paths.get(originalDirectory, relativePath).toString()), targetFile);
                            System.out.println("Restored: " + targetFile.toString());
                        } else {
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
                        targetFile.delete();

                        if (new File(Paths.get(originalDirectory, specialFile.getTargetFileName()).toString()).exists()) {
                            FileUtils.moveFile(new File(Paths.get(originalDirectory, specialFile.getTargetFileName()).toString()), targetFile);
                            System.out.println("Restored: " + targetFile.toString());
                        } else {
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

    /**
     * @param id of the desired ModBundle
     * @return whether the ModBundle exists and is compatible with the game version
     */
    public boolean isCompatible(String id) {
        ModBundle bundle = loadedBundles.get(id);

        if (bundle != null) {
            return bundle.getGameVersions().keySet().contains(configObject.getString("game_version")) ||
                    bundle.getGameVersions().keySet().contains("any");
        } else {
            return false;
        }
    }

    /**
     * Load a ModBundle from the given ID
     *
     * @param id of the desired ModBundle
     * @throws IOException for any failure in reading or writing the file, including those thrown by MD5Util
     * @throws NoSuchAlgorithmException if an error occurs in MD5Util
     */
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

    /** @return the collection of all ModBundles loaded */
    public Collection<ModBundle> getModBundles() {
        return loadedBundles.values();
    }

    /** @return the ModBundle whose ID matches the one given */
    public ModBundle getModBundle(String id) {
        return loadedBundles.get(id);
    }

    /** @return the current ModBundle as given by the database config */
    public ModBundle getCurrentModBundle() {
        return configObject.isNull("current_bundle") ? null : getModBundle(configObject.getString("current_bundle"));
    }

    /**
     * Set the path to Assembly-CSharp.dll, and modify the game version accordingly
     *
     * @param path the location of the game assembly
     * @throws IOException for any failure by FileUtils or saveConfig
     * @throws NoSuchAlgorithmException for problems with MD5Util
     */
    public void setOriginalAssembly(String path) throws IOException, NoSuchAlgorithmException {
        configObject.put("original_assembly", path);
        configObject.put("game_version", MD5Util.hashFile(path));
        FileUtils.cleanDirectory(new File(originalDirectory));
        saveConfig();
    }

    /** @return the path to Assembly-CSharp.dll */
    public String getOriginalAssembly() {
        return configObject.isNull("original_assembly") ? null : configObject.getString("original_assembly");
    }

    /** @return the game version hash of the original assembly */
    public String getGameVersion() {
        return configObject.isNull("game_version") ? "---" : configObject.getString("game_version");
    }
}
