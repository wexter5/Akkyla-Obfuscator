package com.obfuscator.core;

import com.obfuscator.transformers.ClassRenamerTransformer;
import com.obfuscator.transformers.ControlFlowObfuscator;
import com.obfuscator.transformers.DecompilerCrasherTransformer;
import com.obfuscator.transformers.DummyClassTransformer;
import com.obfuscator.transformers.StringEncryptionTransformer;
import com.obfuscator.util.JarUtils;
import com.obfuscator.util.MixinUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;

/**
 * Главный класс обфускатора, управляющий процессом обработки JAR файлов.
 */
public class Obfuscator {
    
    private final List<Transformer> transformers;
    private final ObfuscationOptions options;
    private final ObfuscationContext context;
    private final ProgressListener progressListener;
    
    public interface ProgressListener {
        void onProgress(int current, int total, String message);
        void onError(String error);
        void onComplete(String message);
    }
    
    public Obfuscator(ObfuscationOptions options, ProgressListener listener) {
        this.options = options;
        this.context = new ObfuscationContext(options);
        this.progressListener = listener;
        this.transformers = new ArrayList<>();
        
        initTransformers();
    }
    
    private void initTransformers() {
        if (options.isStringEncryption()) {
            transformers.add(new StringEncryptionTransformer());
        }
        if (options.isControlFlowObfuscation()) {
            transformers.add(new ControlFlowObfuscator());
        }
        if (options.isDecompilerCrasher()) {
            transformers.add(new DecompilerCrasherTransformer());
        }
        if (options.isRenameClasses()) {
            transformers.add(new ClassRenamerTransformer());
        }
    }
    
    /**
     * Обфусцирует JAR файл.
     */
    public void obfuscate(File inputJar, File outputJar) throws IOException {
        if (!inputJar.exists()) {
            throw new FileNotFoundException("Input JAR not found: " + inputJar.getAbsolutePath());
        }
        
        log("Starting obfuscation process...");
        log("Input: " + inputJar.getAbsolutePath());
        log("Output: " + outputJar.getAbsolutePath());
        
        // Читаем все классы из JAR
        Map<String, byte[]> classFiles = new HashMap<>();
        Map<String, byte[]> otherFiles = new HashMap<>();
        
        readJarContents(inputJar, classFiles, otherFiles);
        
        log("Found " + classFiles.size() + " classes and " + otherFiles.size() + " other files");

        // Обфусцируем классы
        Map<String, byte[]> obfuscatedClasses = new HashMap<>();
        int total = classFiles.size();
        int current = 0;
        
        log("Starting class obfuscation...");

        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            String className = entry.getKey();
            byte[] classBytes = entry.getValue();

            current++;
            if (current % 50 == 0) {
                log("Progress: " + current + "/" + total);
            }
            
            if (progressListener != null) {
                progressListener.onProgress(current, total, "Processing: " + className);
            }

            try {
                byte[] obfuscated = obfuscateClass(className, classBytes);
                // Используем новое имя класса если оно есть в маппинге
                String newName = context.getClassMapping().get(className);
                if (newName != null && !newName.equals(className)) {
                    obfuscatedClasses.put(newName, obfuscated);
                } else {
                    obfuscatedClasses.put(className, obfuscated);
                }
            } catch (Exception e) {
                log("Error processing class " + className + ": " + e.getMessage());
                // Сохраняем оригинальный класс при ошибке
                obfuscatedClasses.put(className, classBytes);
            }
        }
        
        log("Class obfuscation completed. Adding dummy classes...");

        // Добавляем фейковые классы если включено
        if (options.isAddDummyClasses()) {
            DummyClassTransformer dummyTransformer = new DummyClassTransformer();
            Map<String, byte[]> dummyClasses = new HashMap<>();
            
            // Загружаем из папки если указано
            if (options.isImportDummyClasses() && !options.getDummyClassesFolder().isEmpty()) {
                log("Loading dummy classes from: " + options.getDummyClassesFolder());
                dummyClasses.putAll(dummyTransformer.loadDummyClassesFromFolder(options.getDummyClassesFolder()));
            }
            
            // Добавляем сгенерированные классы
            if (options.getDummyClassCount() > 0) {
                log("Generating " + options.getDummyClassCount() + " dummy classes...");
                dummyClasses.putAll(dummyTransformer.generateDummyClasses(options.getDummyClassCount()));
            }
            
            obfuscatedClasses.putAll(dummyClasses);
            log("Added " + dummyClasses.size() + " dummy classes");
        }

        // Обновляем fabric.mod.json и mods.toml с новыми именами классов
        updateModMetadata(otherFiles, context);

        // Создаём выходной JAR
        log("Writing output JAR...");
        createOutputJar(outputJar, obfuscatedClasses, otherFiles, inputJar);
        
        log("Obfuscation complete!");
        
        if (progressListener != null) {
            progressListener.onComplete("Obfuscation completed successfully!");
        }
    }
    
    /**
     * Читает содержимое JAR файла.
     */
    private void readJarContents(File jarFile, Map<String, byte[]> classFiles,
                                  Map<String, byte[]> otherFiles) throws IOException {
        try (JarFile jf = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jf.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();

                // Игнорируем META-INF (кроме манифеста) и служебные файлы
                if (name.startsWith("META-INF/") && !name.equals("META-INF/MANIFEST.MF")) {
                    continue;
                }

                // Игнорируем файлы подписи
                if (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA")) {
                    continue;
                }

                try (InputStream is = jf.getInputStream(entry)) {
                    byte[] content = is.readAllBytes();

                    if (name.endsWith(".class")) {
                        // Убираем .class из имени для ключа
                        String className = name.substring(0, name.length() - 6);
                        classFiles.put(className, content);
                    } else if (name.equals("fabric.mod.json") || name.equals("mods.toml")) {
                        // Сохраняем конфиги модов для последующей обработки
                        otherFiles.put(name, content);
                    } else if (!name.equals("META-INF/MANIFEST.MF")) {
                        // Сохраняем другие файлы (ресурсы, конфиги и т.д.)
                        otherFiles.put(name, content);
                    }
                } catch (IOException e) {
                    log("Warning: Could not read entry " + name + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Обновляет fabric.mod.json и mods.toml с новыми именами классов.
     */
    private void updateModMetadata(Map<String, byte[]> otherFiles, ObfuscationContext context) {
        Map<String, String> classMapping = context.getClassMapping();
        
        if (classMapping == null || classMapping.isEmpty()) {
            return;
        }
        
        // Обновляем fabric.mod.json
        if (otherFiles.containsKey("fabric.mod.json")) {
            try {
                String content = new String(otherFiles.get("fabric.mod.json"), "UTF-8");
                List<Map.Entry<String, String>> sorted = new ArrayList<>(classMapping.entrySet());
                sorted.sort((a, b) -> b.getKey().length() - a.getKey().length());
                
                for (Map.Entry<String, String> entry : sorted) {
                    String oldName = entry.getKey().replace("/", ".");
                    String newName = entry.getValue().replace("/", ".");
                    content = content.replace(oldName, newName);
                }
                
                otherFiles.put("fabric.mod.json", content.getBytes("UTF-8"));
            } catch (Exception e) {
                // Игнорируем ошибки JSON
            }
        }
        
        // Обновляем mods.toml (Forge)
        if (otherFiles.containsKey("mods.toml")) {
            try {
                String content = new String(otherFiles.get("mods.toml"), "UTF-8");
                List<Map.Entry<String, String>> sorted = new ArrayList<>(classMapping.entrySet());
                sorted.sort((a, b) -> b.getKey().length() - a.getKey().length());
                
                for (Map.Entry<String, String> entry : sorted) {
                    String oldName = entry.getKey().replace("/", ".");
                    String newName = entry.getValue().replace("/", ".");
                    content = content.replace(oldName, newName);
                }
                
                otherFiles.put("mods.toml", content.getBytes("UTF-8"));
            } catch (Exception e) {
                // Игнорируем ошибки TOML
            }
        }
    }
    
    /**
     * Обфусцирует отдельный класс.
     */
    private byte[] obfuscateClass(String className, byte[] classBytes) {
        // Проверяем исключения
        if (context.isExcluded(className)) {
            return classBytes;
        }
        
        // Проверяем Mixin классы
        if (options.isExcludeMixins()) {
            try {
                ClassReader reader = new ClassReader(classBytes);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);
                
                if (MixinUtils.isMixinClass(classNode) || MixinUtils.isInMixinPackage(className)) {
                    log("Skipping Mixin class: " + className);
                    return classBytes;
                }
            } catch (Exception e) {
                log("Warning: Could not parse class " + className);
                return classBytes;
            }
        }
        
        // Читаем класс
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        
        // Парсим класс
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        
        // Применяем трансформеры
        String newClassName = className;

        for (Transformer transformer : transformers) {
            if (transformer.shouldTransform(className, context)) {
                try {
                    boolean classChanged = transformer.transform(classNode, context);
                    if (classChanged && !classNode.name.equals(className)) {
                        newClassName = classNode.name;
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки в трансформерах
                }
            }
        }

        // Записываем класс
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        try {
            classNode.accept(writer);
            byte[] result = writer.toByteArray();
            
            // Возвращаем класс с новым именем если оно изменилось
            if (!newClassName.equals(className)) {
                context.getClassMapping().put(className, newClassName);
            }
            return result;
        } catch (Exception e) {
            log("Error writing class " + className + ": " + e.getMessage());
            // Возвращаем оригинальный класс при ошибке записи
            return classBytes;
        }
    }
    
    /**
     * Создаёт выходной JAR файл.
     */
    private void createOutputJar(File outputFile, Map<String, byte[]> classes,
                                  Map<String, byte[]> otherFiles, File sourceJar) throws IOException {
        // Копируем манифест из исходного JAR
        Manifest manifest = null;
        try {
            manifest = JarUtils.getManifest(sourceJar);
        } catch (IOException e) {
            log("Warning: Could not read manifest from source JAR");
        }
        
        if (manifest == null) {
            manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        }
        
        // Добавляем информацию об обфускаторе
        manifest.getMainAttributes().put(new Attributes.Name("Obfuscated-By"), "Minecraft Obfuscator");
        manifest.getMainAttributes().put(new Attributes.Name("Obfuscation-Date"), new Date().toString());
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile), manifest)) {
            // Записываем другие файлы (не классы)
            for (Map.Entry<String, byte[]> entry : otherFiles.entrySet()) {
                String name = entry.getKey();
                byte[] content = entry.getValue();
                
                JarEntry jarEntry = new JarEntry(name);
                jos.putNextEntry(jarEntry);
                jos.write(content);
                jos.closeEntry();
            }
            
            // Записываем классы
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
                String className = entry.getKey();
                byte[] content = entry.getValue();
                
                String entryName = className + ".class";
                JarEntry jarEntry = new JarEntry(entryName);
                jos.putNextEntry(jarEntry);
                jos.write(content);
                jos.closeEntry();
            }
        }
    }
    
    private void log(String message) {
        System.out.println("[Obfuscator] " + message);
        if (progressListener != null) {
            // Можно добавить метод onLog для передачи сообщений в GUI
        }
    }
    
    public ObfuscationContext getContext() {
        return context;
    }
    
    public ObfuscationOptions getOptions() {
        return options;
    }
}
