package com.obfuscator.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Контекст обфускации, хранящий состояние и настройки для всех трансформеров.
 */
public class ObfuscationContext {
    
    private final Set<String> excludedClasses;
    private final Set<String> excludedPackages;
    private final Map<String, String> classMapping;
    private final Map<String, String> methodMapping;
    private final Map<String, String> fieldMapping;
    private final Random random;
    private final Set<String> mixinPackages;
    private final ObfuscationOptions options;
    
    public ObfuscationContext(ObfuscationOptions options) {
        this.options = options;
        this.excludedClasses = ConcurrentHashMap.newKeySet();
        this.excludedPackages = ConcurrentHashMap.newKeySet();
        this.classMapping = new ConcurrentHashMap<>();
        this.methodMapping = new ConcurrentHashMap<>();
        this.fieldMapping = new ConcurrentHashMap<>();
        this.random = new Random();
        this.mixinPackages = new HashSet<>(Arrays.asList(
            "com/example/mod/mixins/",
            "net/minecraftforge/",
            "net/fabricmc/"
        ));
        
        // Стандартные исключения для Minecraft модов
        addExcludedPackage("org/spongepowered/asm/mixin/");
        addExcludedPackage("com/example/mod/mixins/");
        
        // Исключения библиотек (автоматически)
        addExcludedPackage("oshi/");
        addExcludedPackage("dev/redstones/");
        addExcludedPackage("net/java/");
        addExcludedPackage("com/sun/");
        addExcludedPackage("com/google/");
        addExcludedPackage("org/lwjgl/");
        addExcludedPackage("org/objectweb/asm/");
        addExcludedPackage("io/netty/");
        addExcludedPackage("kotlin/");
        addExcludedPackage("kotlinx/");
        addExcludedPackage("javax/");
        addExcludedPackage("jdk/");
        addExcludedPackage("sun/");
        addExcludedPackage("com/mojang/");
        addExcludedPackage("net/minecraft/");
        addExcludedPackage("gnu/it/unimi/");
        addExcludedPackage("org/apache/");
        addExcludedPackage("com/nimbusds/");
        addExcludedPackage("org/joml/");
        addExcludedPackage("org/xmlsoap/");
        addExcludedPackage("org/w3c/");
        addExcludedPackage("javax/");
        addExcludedPackage("javafx/");
        addExcludedPackage("com/squareup/");
        addExcludedPackage("okhttp3/");
        addExcludedPackage("okio/");
        addExcludedPackage("retrofit2/");
        addExcludedPackage("com/github/");
        addExcludedPackage("org/json/");
        addExcludedPackage("com/jcraft/");
        addExcludedPackage("de/oceanlabs/");
        addExcludedPackage("com/jagrosh/");
        addExcludedPackage("net/dv8tion/");
        addExcludedPackage("com/googlecode/");
        addExcludedPackage("org/tuka/");
        addExcludedPackage("com/vdurmont/");
    }
    
    /**
     * Проверяет, исключён ли класс из обфускации.
     */
    public boolean isExcluded(String className) {
        // Проверяем точное совпадение
        if (excludedClasses.contains(className)) {
            return true;
        }
        
        // Проверяем пакеты
        for (String pkg : excludedPackages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        
        // Проверяем Mixin классы
        if (className.contains("mixin") || className.endsWith("Mixin")) {
            return options.isExcludeMixins();
        }
        
        // Проверяем аннотации Mixin (будет проверяться отдельно)
        return false;
    }
    
    public void addExcludedClass(String className) {
        excludedClasses.add(normalizeClassName(className));
    }
    
    public void addExcludedPackage(String packageName) {
        excludedPackages.add(normalizeClassName(packageName));
    }
    
    public void addExclusionPattern(String pattern) {
        pattern = pattern.trim();
        if (pattern.isEmpty()) return;
        
        if (pattern.endsWith("*")) {
            // Паттерн пакета
            addExcludedPackage(pattern.replace("*", "").replace(".", "/"));
        } else if (pattern.endsWith(".java") || pattern.endsWith(".class")) {
            // Имя класса
            addExcludedClass(pattern.replace(".java", "").replace(".class", "").replace(".", "/"));
        } else {
            // Может быть класс или пакет
            addExcludedClass(pattern.replace(".", "/"));
        }
    }
    
    private String normalizeClassName(String className) {
        return className.replace(".", "/").replace("\\", "/");
    }
    
    public Random getRandom() {
        return random;
    }
    
    public Map<String, String> getClassMapping() {
        return classMapping;
    }
    
    public Map<String, String> getMethodMapping() {
        return methodMapping;
    }
    
    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }
    
    public ObfuscationOptions getOptions() {
        return options;
    }
    
    public Set<String> getMixinPackages() {
        return mixinPackages;
    }
    
    /**
     * Генерирует случайное имя для обфускации.
     */
    public String generateObfuscatedName(String prefix) {
        String[] obfuscationChars = {
            "IlIIlIlI", "lIlIlIlI", "IlIlIlIl", "llIlIlIl",
            "IllIlIll", "lIllIlIl", "IlIllIll", "llIllIll"
        };
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            sb.append(obfuscationChars[random.nextInt(obfuscationChars.length)]);
        }
        return sb.toString();
    }
}
