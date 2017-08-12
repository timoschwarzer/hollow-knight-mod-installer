package com.timoschwarzer.hkmodinstaller.data;

public class SpecialFile {
    private String fileName;
    private String targetFileName;
    private String targetGameHash;

    public SpecialFile(String fileName, String targetFileName, String targetGameHash) {
        this.fileName = fileName;
        this.targetFileName = targetFileName;
        this.targetGameHash = targetGameHash;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public String getTargetGameHash() {
        return targetGameHash;
    }
}
