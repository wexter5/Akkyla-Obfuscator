package com.obfuscator;

import java.util.*;

/**
 * Конфигурация обфускатора
 */
public class ObfuscatorConfig {
    private String inputJar;
    private String outputJar;
    private boolean renameEnabled = true;
    private boolean stringEncryptionEnabled = true;
    private boolean flowObfuscationEnabled = true;
    private List<String> excludeClasses = new ArrayList<>();
    private List<String> excludeMethods = new ArrayList<>();
    
    // Настройки renaming
    private boolean useSingleLetterNames = true;
    private boolean obfuscateStrings = true;
    
    // Настройки flow obfuscation
    private int flowComplexity = 3; // Количество фиктивных переходов
    
    public String getInputJar() { return inputJar; }
    public void setInputJar(String inputJar) { this.inputJar = inputJar; }
    
    public String getOutputJar() { return outputJar; }
    public void setOutputJar(String outputJar) { this.outputJar = outputJar; }
    
    public boolean isRenameEnabled() { return renameEnabled; }
    public void setRenameEnabled(boolean renameEnabled) { this.renameEnabled = renameEnabled; }
    
    public boolean isStringEncryptionEnabled() { return stringEncryptionEnabled; }
    public void setStringEncryptionEnabled(boolean stringEncryptionEnabled) { this.stringEncryptionEnabled = stringEncryptionEnabled; }
    
    public boolean isFlowObfuscationEnabled() { return flowObfuscationEnabled; }
    public void setFlowObfuscationEnabled(boolean flowObfuscationEnabled) { this.flowObfuscationEnabled = flowObfuscationEnabled; }
    
    public List<String> getExcludeClasses() { return excludeClasses; }
    public void setExcludeClasses(List<String> excludeClasses) { this.excludeClasses = excludeClasses; }
    
    public List<String> getExcludeMethods() { return excludeMethods; }
    public void setExcludeMethods(List<String> excludeMethods) { this.excludeMethods = excludeMethods; }
    
    public boolean isUseSingleLetterNames() { return useSingleLetterNames; }
    public void setUseSingleLetterNames(boolean useSingleLetterNames) { this.useSingleLetterNames = useSingleLetterNames; }
    
    public int getFlowComplexity() { return flowComplexity; }
    public void setFlowComplexity(int flowComplexity) { this.flowComplexity = flowComplexity; }
}
