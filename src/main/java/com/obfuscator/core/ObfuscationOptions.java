package com.obfuscator.core;

/**
 * Настройки обфускации.
 */
public class ObfuscationOptions {
    
    private boolean stringEncryption;
    private boolean controlFlowObfuscation;
    private boolean decompilerCrasher;
    private boolean renameClasses;
    private boolean excludeMixins;
    private boolean removeLineNumberTable;
    private boolean removeLocalVariableTable;
    private boolean addSyntheticFlags;
    private boolean insertNops;
    private boolean addDummyClasses;
    private int dummyClassCount;
    private String dummyClassesFolder;
    private boolean importDummyClasses;
    
    public ObfuscationOptions() {
        // Значения по умолчанию
        this.stringEncryption = false; // Отключено по умолчанию (проблемы с байт-кодом)
        this.controlFlowObfuscation = true;
        this.decompilerCrasher = true;
        this.renameClasses = true;
        this.excludeMixins = true;
        this.removeLineNumberTable = true;
        this.removeLocalVariableTable = true;
        this.addSyntheticFlags = true;
        this.insertNops = true;
        this.addDummyClasses = true;
        this.dummyClassCount = 10;
        this.dummyClassesFolder = "";
        this.importDummyClasses = false;
    }
    
    // Getters и setters
    
    public boolean isStringEncryption() {
        return stringEncryption;
    }
    
    public void setStringEncryption(boolean stringEncryption) {
        this.stringEncryption = stringEncryption;
    }
    
    public boolean isControlFlowObfuscation() {
        return controlFlowObfuscation;
    }
    
    public void setControlFlowObfuscation(boolean controlFlowObfuscation) {
        this.controlFlowObfuscation = controlFlowObfuscation;
    }
    
    public boolean isDecompilerCrasher() {
        return decompilerCrasher;
    }
    
    public void setDecompilerCrasher(boolean decompilerCrasher) {
        this.decompilerCrasher = decompilerCrasher;
    }
    
    public boolean isRenameClasses() {
        return renameClasses;
    }
    
    public void setRenameClasses(boolean renameClasses) {
        this.renameClasses = renameClasses;
    }
    
    public boolean isExcludeMixins() {
        return excludeMixins;
    }
    
    public void setExcludeMixins(boolean excludeMixins) {
        this.excludeMixins = excludeMixins;
    }
    
    public boolean isRemoveLineNumberTable() {
        return removeLineNumberTable;
    }
    
    public void setRemoveLineNumberTable(boolean removeLineNumberTable) {
        this.removeLineNumberTable = removeLineNumberTable;
    }
    
    public boolean isRemoveLocalVariableTable() {
        return removeLocalVariableTable;
    }
    
    public void setRemoveLocalVariableTable(boolean removeLocalVariableTable) {
        this.removeLocalVariableTable = removeLocalVariableTable;
    }
    
    public boolean isAddSyntheticFlags() {
        return addSyntheticFlags;
    }
    
    public void setAddSyntheticFlags(boolean addSyntheticFlags) {
        this.addSyntheticFlags = addSyntheticFlags;
    }
    
    public boolean isInsertNops() {
        return insertNops;
    }
    
    public void setInsertNops(boolean insertNops) {
        this.insertNops = insertNops;
    }

    public boolean isAddDummyClasses() {
        return addDummyClasses;
    }
    
    public void setAddDummyClasses(boolean addDummyClasses) {
        this.addDummyClasses = addDummyClasses;
    }
    
    public int getDummyClassCount() {
        return dummyClassCount;
    }
    
    public void setDummyClassCount(int dummyClassCount) {
        this.dummyClassCount = dummyClassCount;
    }
    
    public String getDummyClassesFolder() {
        return dummyClassesFolder;
    }
    
    public void setDummyClassesFolder(String dummyClassesFolder) {
        this.dummyClassesFolder = dummyClassesFolder;
    }
    
    public boolean isImportDummyClasses() {
        return importDummyClasses;
    }
    
    public void setImportDummyClasses(boolean importDummyClasses) {
        this.importDummyClasses = importDummyClasses;
    }
}
