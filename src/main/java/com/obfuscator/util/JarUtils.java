package com.obfuscator.util;

import java.io.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * Утилиты для работы с JAR файлами.
 */
public class JarUtils {
    
    /**
     * Копирует JAR файл.
     */
    public static void copyJar(File source, File dest) throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(source));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(dest))) {
            
            byte[] buffer = new byte[8192];
            JarEntry entry;
            
            while ((entry = jis.getNextJarEntry()) != null) {
                jos.putNextEntry(new JarEntry(entry.getName()));
                
                int count;
                while ((count = jis.read(buffer)) > 0) {
                    jos.write(buffer, 0, count);
                }
                
                jos.closeEntry();
            }
        }
    }
    
    /**
     * Читает все классы из JAR файла.
     * 
     * @param jarFile JAR файл
     * @return массив байтов классов
     */
    public static byte[][] readClasses(File jarFile) throws IOException {
        java.util.List<byte[]> classes = new java.util.ArrayList<>();
        
        try (JarFile jf = new JarFile(jarFile)) {
            var entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
                    try (InputStream is = jf.getInputStream(entry)) {
                        classes.add(is.readAllBytes());
                    } catch (ZipException e) {
                        // Пропускаем повреждённые entries
                    }
                }
            }
        }
        
        return classes.toArray(new byte[0][]);
    }
    
    /**
     * Создаёт JAR файл с классами.
     */
    public static void createJar(File outputFile, java.util.Map<String, byte[]> classes) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile))) {
            for (var entry : classes.entrySet()) {
                String name = entry.getKey();
                if (!name.endsWith(".class")) {
                    name += ".class";
                }
                
                JarEntry jarEntry = new JarEntry(name);
                jos.putNextEntry(jarEntry);
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
    }
    
    /**
     * Копирует не-class файлы из исходного JAR в выходной.
     */
    public static void copyNonClassEntries(File sourceJar, File destJar) throws IOException {
        File tempFile = File.createTempFile("obf_temp", ".jar");
        tempFile.deleteOnExit();
        
        try (JarInputStream jis = new JarInputStream(new FileInputStream(sourceJar));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempFile))) {
            
            byte[] buffer = new byte[8192];
            JarEntry entry;
            
            while ((entry = jis.getNextJarEntry()) != null) {
                if (!entry.getName().endsWith(".class")) {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    
                    int count;
                    while ((count = jis.read(buffer)) > 0) {
                        jos.write(buffer, 0, count);
                    }
                    
                    jos.closeEntry();
                }
            }
        }
        
        // Добавляем классы из destJar
        try (JarFile jf = new JarFile(destJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempFile, true))) {
            
            var entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    
                    try (InputStream is = jf.getInputStream(entry)) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = is.read(buffer)) > 0) {
                            jos.write(buffer, 0, count);
                        }
                    }
                    
                    jos.closeEntry();
                }
            }
        }
        
        // Перемещаем временный файл в destJar
        if (destJar.exists()) {
            destJar.delete();
        }
        tempFile.renameTo(destJar);
    }
    
    /**
     * Проверяет, является ли файл валидным JAR.
     */
    public static boolean isValidJar(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        try (JarFile jf = new JarFile(file)) {
            return jf.entries().hasMoreElements();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Получает манифест из JAR файла.
     */
    public static Manifest getManifest(File jarFile) throws IOException {
        try (JarFile jf = new JarFile(jarFile)) {
            return jf.getManifest();
        }
    }
}
