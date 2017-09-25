package com.timoschwarzer.hkmodinstaller.data;

/**
 * A specialized container for mod file information used in ModBundles.
 */
public class SpecialFile {
    private String fileName;
    private String targetFileName;
    private String targetGameHash;

    public SpecialFile(String fileName, String targetFileName, String targetGameHash) {
        this.fileName = fileName;
        this.targetFileName = targetFileName;
        this.targetGameHash = targetGameHash;
    }

    /** @return the SpecialFile's name */
    public String getFileName() {
        return fileName;
    }

    /** @return the name of the SpecialFile's target*/
    public String getTargetFileName() {
        return targetFileName;
    }

    /** @return the hash of the target file's game version */
    public String getTargetGameHash() {
        return targetGameHash;
    }
}
