package com.obfuscator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.obfuscator.transformers.FlowObfuscationTransformer;
import com.obfuscator.transformers.RenamingTransformer;
import com.obfuscator.transformers.StringEncryptionTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/**
 * Главный класс обфускатора
 */
public class Main {
    
    @Parameter(names = {"-i", "--input"}, description = "Input JAR file", required = true)
    private String inputJar;
    
    @Parameter(names = {"-o", "--output"}, description = "Output JAR file", required = true)
    private String outputJar;
    
    @Parameter(names = {"--no-rename"}, description = "Disable renaming transformer")
    private boolean noRename = false;
    
    @Parameter(names = {"--no-string"}, description = "Disable string encryption transformer")
    private boolean noString = false;
    
    @Parameter(names = {"--no-flow"}, description = "Disable flow obfuscation transformer")
    private boolean noFlow = false;
    
    @Parameter(names = {"--help", "-h"}, description = "Show help", help = true)
    private boolean help = false;
    
    @Parameter(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose = false;

    @Parameter(names = {"-gui"}, description = "Launch GUI interface")
    private boolean guiMode = false;

    public static void main(String[] args) {
        Main main = new Main();
        JCommander jCommander = JCommander.newBuilder()
            .addObject(main)
            .build();

        try {
            jCommander.parse(args);
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            jCommander.usage();
            System.exit(1);
        }

        // Запуск GUI
        if (main.guiMode || args.length == 0) {
            launchGUI();
            return;
        }

        if (main.help) {
            jCommander.usage();
            System.exit(0);
        }

        main.run();
    }

    private static void launchGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            com.obfuscator.ui.ObfuscatorGUI gui = new com.obfuscator.ui.ObfuscatorGUI();
            gui.setVisible(true);
        });
    }
    
    private void run() {
        System.out.println("========================================");
        System.out.println("     Java Obfuscator v1.0");
        System.out.println("========================================");
        
        ObfuscatorConfig config = new ObfuscatorConfig();
        config.setInputJar(inputJar);
        config.setOutputJar(outputJar);
        config.setRenameEnabled(!noRename);
        config.setStringEncryptionEnabled(!noString);
        config.setFlowObfuscationEnabled(!noFlow);
        
        try {
            // Чтение JAR
            System.out.println("\n[1/4] Reading JAR file: " + inputJar);
            Map<String, ClassNode> classes = new HashMap<>();
            Map<String, byte[]> resources = new HashMap<>();
            Manifest manifest = readJar(inputJar, classes, resources);
            
            System.out.println("  Found " + classes.size() + " classes");
            System.out.println("  Found " + resources.size() + " resources");
            
            // Создаём трансформеры
            System.out.println("\n[2/4] Initializing transformers...");
            List<Transformer> transformers = new ArrayList<>();
            
            RenamingTransformer renamingTransformer = null;
            
            if (config.isRenameEnabled()) {
                renamingTransformer = new RenamingTransformer(config);
                transformers.add(renamingTransformer);
                System.out.println("  + RenamingTransformer (enabled)");
            } else {
                System.out.println("  - RenamingTransformer (disabled)");
            }
            
            if (config.isStringEncryptionEnabled()) {
                transformers.add(new StringEncryptionTransformer(config));
                System.out.println("  + StringEncryptionTransformer (enabled)");
            } else {
                System.out.println("  - StringEncryptionTransformer (disabled)");
            }
            
            if (config.isFlowObfuscationEnabled()) {
                transformers.add(new FlowObfuscationTransformer(config));
                System.out.println("  + FlowObfuscationTransformer (enabled)");
            } else {
                System.out.println("  - FlowObfuscationTransformer (disabled)");
            }
            
            // Применяем трансформеры
            System.out.println("\n[3/4] Applying transformers...");
            for (Transformer transformer : transformers) {
                System.out.println("\n  [" + transformer.getName() + "] Starting");
                int modifiedCount = 0;
                
                for (ClassNode classNode : classes.values()) {
                    if (transformer.transform(classNode, classes)) {
                        modifiedCount++;
                    }
                }
                
                System.out.println("  [" + transformer.getName() + "] Done - Modified " + modifiedCount + " classes");
            }
            
            // Записываем результат
            System.out.println("\n[4/4] Writing output JAR: " + outputJar);
            writeJar(outputJar, manifest, classes, resources, renamingTransformer);
            
            System.out.println("\n========================================");
            System.out.println("     Obfuscation complete!");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("\nError during obfuscation:");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Чтение JAR файла
     */
    private Manifest readJar(String jarPath, Map<String, ClassNode> classes, Map<String, byte[]> resources) throws IOException {
        Manifest manifest = null;
        
        try (JarFile jarFile = new JarFile(jarPath)) {
            manifest = jarFile.getManifest();
            
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                if (entry.isDirectory()) {
                    continue;
                }
                
                try (InputStream is = jarFile.getInputStream(entry)) {
                    byte[] data = is.readAllBytes();
                    
                    if (name.endsWith(".class")) {
                        // Это класс
                        ClassReader reader = new ClassReader(data);
                        ClassNode classNode = new ClassNode();
                        reader.accept(classNode, 0);
                        classes.put(classNode.name, classNode);
                    } else {
                        // Это ресурс
                        resources.put(name, data);
                    }
                }
            }
        }
        
        return manifest;
    }
    
    /**
     * Запись JAR файла
     */
    private void writeJar(String jarPath, Manifest manifest, Map<String, ClassNode> classes, 
                          Map<String, byte[]> resources, RenamingTransformer renamingTransformer) throws IOException {
        
        // Создаём директорию если нужно
        Path outputPath = Paths.get(jarPath);
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath), manifest)) {

            // Добавляем класс-дешифратор если использовалось шифрование строк
            ClassNode decryptorClass = StringEncryptionTransformer.createDecryptorClass();
            ClassWriter decryptorCw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            decryptorClass.accept(decryptorCw);
            jos.putNextEntry(new JarEntry(decryptorClass.name + ".class"));
            jos.write(decryptorCw.toByteArray());
            jos.closeEntry();

            // Добавляем обфусцированные классы
            for (ClassNode classNode : classes.values()) {

                // Применяем Remapper если есть
                ClassWriter classCw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                if (renamingTransformer != null) {
                    org.objectweb.asm.commons.ClassRemapper remapper =
                        new org.objectweb.asm.commons.ClassRemapper(classCw, renamingTransformer.getRemapper());
                    classNode.accept(remapper);
                } else {
                    classNode.accept(classCw);
                }

                byte[] classData = classCw.toByteArray();
                
                // ClassRemapper уже переименовал класс внутри classCw
                String className = classNode.name;

                jos.putNextEntry(new JarEntry(className + ".class"));
                jos.write(classData);
                jos.closeEntry();
            }
            
            // Добавляем ресурсы
            for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
                String name = entry.getKey();

                // Пропускаем signature файлы и MANIFEST
                if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") || name.equals("META-INF/MANIFEST.MF"))) {
                    continue;
                }

                jos.putNextEntry(new JarEntry(name));
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
    }
}
