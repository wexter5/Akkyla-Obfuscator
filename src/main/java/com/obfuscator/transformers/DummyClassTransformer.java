package com.obfuscator.transformers;

import com.obfuscator.core.ObfuscationContext;
import com.obfuscator.core.Transformer;
import com.obfuscator.util.DummyClassGenerator;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/**
 * Добавляет фейковые (dummy) классы в JAR для запутывания.
 */
public class DummyClassTransformer implements Transformer {
    
    private static final String[] DUMMY_CLASS_NAMES = {
        "com/obf/Dummy",
        "com/obf/Empty",
        "com/obf/Fake",
        "com/obf/Stub",
        "com/obf/Mock",
        "com/obf/Test",
        "com/obf/Util",
        "com/obf/Helper",
        "com/obf/Service",
        "com/obf/Manager"
    };
    
    private final List<String> addedClasses = new ArrayList<>();
    
    @Override
    public String getName() {
        return "DummyClass";
    }
    
    @Override
    public boolean shouldTransform(String className, ObfuscationContext context) {
        // Этот трансформер не трансформирует классы, а добавляет новые
        return false;
    }
    
    @Override
    public boolean transform(ClassNode classNode, ObfuscationContext context) {
        // Не трансформируем существующие классы
        return false;
    }
    
    /**
     * Генерирует коллекцию фейковых классов для добавления в JAR.
     */
    public Map<String, byte[]> generateDummyClasses(int count) {
        Map<String, byte[]> dummyClasses = new HashMap<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            String baseName = DUMMY_CLASS_NAMES[random.nextInt(DUMMY_CLASS_NAMES.length)];
            String className = baseName + "_" + i + "_" + System.nanoTime();
            
            // Выбираем тип генерируемого класса
            int type = random.nextInt(3);
            byte[] classBytes;
            
            switch (type) {
                case 0:
                    classBytes = DummyClassGenerator.generateDummyClass(className);
                    break;
                case 1:
                    classBytes = DummyClassGenerator.generateEmptyClass(className);
                    break;
                case 2:
                    classBytes = DummyClassGenerator.generateLongNameClass(className);
                    break;
                default:
                    classBytes = DummyClassGenerator.generateDummyClass(className);
            }
            
            dummyClasses.put(className, classBytes);
            addedClasses.add(className);
        }
        
        return dummyClasses;
    }
    
    /**
     * Загружает dummy классы из указанной папки.
     */
    public Map<String, byte[]> loadDummyClassesFromFolder(String folderPath) {
        Map<String, byte[]> dummyClasses = new HashMap<>();
        File folder = new File(folderPath);
        
        if (!folder.exists() || !folder.isDirectory()) {
            return dummyClasses;
        }
        
        // Загружаем .class файлы
        loadClassFiles(folder, dummyClasses);
        
        // Загружаем .jar файлы
        loadJarFiles(folder, dummyClasses);
        
        return dummyClasses;
    }
    
    private void loadClassFiles(File folder, Map<String, byte[]> dummyClasses) {
        File[] classFiles = folder.listFiles((dir, name) -> name.endsWith(".class"));
        if (classFiles != null) {
            for (File classFile : classFiles) {
                try {
                    byte[] classBytes = Files.readAllBytes(classFile.toPath());
                    // Используем имя файла как имя класса (без .class)
                    String className = classFile.getName().replace(".class", "");
                    className = "com/obf/imported/" + className;
                    dummyClasses.put(className, classBytes);
                    addedClasses.add(className);
                } catch (IOException e) {
                    System.err.println("Error loading class file: " + classFile.getName());
                }
            }
        }
    }
    
    private void loadJarFiles(File folder, Map<String, byte[]> dummyClasses) {
        File[] jarFiles = folder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                try (JarFile jf = new JarFile(jarFile)) {
                    Enumeration<JarEntry> entries = jf.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
                            try (InputStream is = jf.getInputStream(entry)) {
                                byte[] classBytes = is.readAllBytes();
                                String className = entry.getName().replace(".class", "");
                                // Переименовываем в obf пакет
                                if (!className.startsWith("com/obf/imported/")) {
                                    className = "com/obf/imported/" + className.replace("/", "_");
                                }
                                dummyClasses.put(className, classBytes);
                                addedClasses.add(className);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error loading jar file: " + jarFile.getName());
                }
            }
        }
    }
    
    public List<String> getAddedClasses() {
        return addedClasses;
    }
}
