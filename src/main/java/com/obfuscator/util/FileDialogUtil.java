package com.obfuscator.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Простой диалог выбора файлов с улучшенным интерфейсом.
 */
public class FileDialogUtil {
    
    /**
     * Показывает диалог открытия файла.
     */
    public static File showOpenDialog(Window parent, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title != null ? title : "Выберите файл");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "JAR Files (*.jar)", "jar"));
        chooser.setAcceptAllFileFilterUsed(false);
        
        int result = chooser.showOpenDialog(parent);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }
    
    /**
     * Показывает диалог сохранения файла.
     */
    public static File showSaveDialog(Window parent, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title != null ? title : "Сохранить файл");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "JAR Files (*.jar)", "jar"));
        chooser.setAcceptAllFileFilterUsed(false);
        
        int result = chooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            // Добавляем .jar если нет
            if (!file.getName().toLowerCase().endsWith(".jar")) {
                file = new File(file.getParentFile(), file.getName() + ".jar");
            }
            return file;
        }
        return null;
    }
}
