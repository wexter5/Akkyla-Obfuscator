package com.obfuscator.ui;

import com.obfuscator.ObfuscatorConfig;
import com.obfuscator.Transformer;
import com.obfuscator.transformers.FlowObfuscationTransformer;
import com.obfuscator.transformers.RenamingTransformer;
import com.obfuscator.transformers.StringEncryptionTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.jar.*;

/**
 * GUI для обфускатора
 */
public class ObfuscatorGUI extends JFrame {
    
    private JTextField inputField;
    private JTextField outputField;
    private JCheckBox renameCheckBox;
    private JCheckBox stringEncryptCheckBox;
    private JCheckBox flowObfuscationCheckBox;
    private JSpinner flowComplexitySpinner;
    private JTextArea logArea;
    private JButton startButton;
    private JProgressBar progressBar;
    
    private ObfuscatorConfig config;
    
    public ObfuscatorGUI() {
        setTitle("Java Obfuscator v1.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        
        config = new ObfuscatorConfig();
        
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // === Верхняя панель ===
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Input файл
        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Input JAR:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        inputField = new JTextField(30);
        topPanel.add(inputField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseInputBtn = new JButton("...");
        browseInputBtn.addActionListener(e -> browseInputFile());
        topPanel.add(browseInputBtn, gbc);
        
        // Output файл
        gbc.gridx = 0; gbc.gridy = 1;
        topPanel.add(new JLabel("Output JAR:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        outputField = new JTextField(30);
        topPanel.add(outputField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseOutputBtn = new JButton("...");
        browseOutputBtn.addActionListener(e -> browseOutputFile());
        topPanel.add(browseOutputBtn, gbc);
        
        add(topPanel, BorderLayout.NORTH);
        
        // === Центральная панель с настройками ===
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        // Настройки трансформеров
        JPanel transformersPanel = new JPanel(new GridBagLayout());
        transformersPanel.setBorder(BorderFactory.createTitledBorder("Transformers"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        renameCheckBox = new JCheckBox("Renaming (переименование классов)", true);
        transformersPanel.add(renameCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        stringEncryptCheckBox = new JCheckBox("String Encryption (шифрование строк)", true);
        transformersPanel.add(stringEncryptCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        flowObfuscationCheckBox = new JCheckBox("Flow Obfuscation (запутывание потока)", true);
        transformersPanel.add(flowObfuscationCheckBox, gbc);
        
        // Flow complexity
        gbc.gridx = 0; gbc.gridy = 3;
        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        flowPanel.add(new JLabel("  Flow complexity: "));
        flowComplexitySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        flowPanel.add(flowComplexitySpinner);
        transformersPanel.add(flowPanel, gbc);
        
        centerPanel.add(transformersPanel, BorderLayout.NORTH);
        
        // Лог
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        
        logArea = new JTextArea(15, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 0));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        logPanel.add(scrollPane, BorderLayout.CENTER);
        
        centerPanel.add(logPanel, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // === Нижняя панель ===
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(200, 20));
        bottomPanel.add(progressBar);
        
        startButton = new JButton("▶ Start Obfuscation");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setBackground(new Color(0, 150, 0));
        startButton.setForeground(Color.WHITE);
        startButton.addActionListener(e -> startObfuscation());
        bottomPanel.add(startButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void browseInputFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
        fileChooser.setDialogTitle("Select Input JAR");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            inputField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            // Автозаполнение output
            String inputPath = fileChooser.getSelectedFile().getAbsolutePath();
            String outputPath = inputPath.replace(".jar", "-obf.jar");
            outputField.setText(outputPath);
        }
    }
    
    private void browseOutputFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
        fileChooser.setDialogTitle("Select Output JAR");
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".jar")) {
                path += ".jar";
            }
            outputField.setText(path);
        }
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void setProgress(int value) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(value));
    }
    
    private void startObfuscation() {
        String inputPath = inputField.getText().trim();
        String outputPath = outputField.getText().trim();
        
        if (inputPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select input JAR file", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (outputPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select output JAR file", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Проверка существования файла
        if (!Files.exists(Paths.get(inputPath))) {
            JOptionPane.showMessageDialog(this, "Input file does not exist: " + inputPath, 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Блокируем кнопку
        startButton.setEnabled(false);
        progressBar.setValue(0);
        logArea.setText("");
        
        // Запускаем в отдельном потоке
        new Thread(() -> {
            try {
                runObfuscation(inputPath, outputPath);
            } catch (Exception e) {
                log("ERROR: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Obfuscation failed:\n" + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    progressBar.setValue(100);
                });
            }
        }).start();
    }
    
    private void runObfuscation(String inputPath, String outputPath) throws Exception {
        log("========================================");
        log("     Java Obfuscator v1.0");
        log("========================================");
        
        config.setRenameEnabled(renameCheckBox.isSelected());
        config.setStringEncryptionEnabled(stringEncryptCheckBox.isSelected());
        config.setFlowObfuscationEnabled(flowObfuscationCheckBox.isSelected());
        config.setFlowComplexity((Integer) flowComplexitySpinner.getValue());
        
        // Чтение JAR
        log("\n[1/4] Reading JAR file: " + inputPath);
        Map<String, ClassNode> classes = new HashMap<>();
        Map<String, byte[]> resources = new HashMap<>();
        Manifest manifest = readJar(inputPath, classes, resources);
        
        log("  Found " + classes.size() + " classes");
        log("  Found " + resources.size() + " resources");
        setProgress(10);
        
        // Создаём трансформеры
        log("\n[2/4] Initializing transformers...");
        List<Transformer> transformers = new ArrayList<>();
        
        RenamingTransformer renamingTransformer = null;
        
        if (config.isRenameEnabled()) {
            renamingTransformer = new RenamingTransformer(config);
            transformers.add(renamingTransformer);
            log("  + RenamingTransformer (enabled)");
        } else {
            log("  - RenamingTransformer (disabled)");
        }
        
        if (config.isStringEncryptionEnabled()) {
            transformers.add(new StringEncryptionTransformer(config));
            log("  + StringEncryptionTransformer (enabled)");
        } else {
            log("  - StringEncryptionTransformer (disabled)");
        }
        
        if (config.isFlowObfuscationEnabled()) {
            transformers.add(new FlowObfuscationTransformer(config));
            log("  + FlowObfuscationTransformer (enabled)");
        } else {
            log("  - FlowObfuscationTransformer (disabled)");
        }
        
        setProgress(25);
        
        // Применяем трансформеры
        log("\n[3/4] Applying transformers...");
        int totalTransformers = transformers.size();
        int currentTransformer = 0;
        
        for (Transformer transformer : transformers) {
            currentTransformer++;
            log("\n  [" + transformer.getName() + "] Starting");
            int modifiedCount = 0;
            
            for (ClassNode classNode : classes.values()) {
                if (transformer.transform(classNode, classes)) {
                    modifiedCount++;
                }
            }
            
            log("  [" + transformer.getName() + "] Done - Modified " + modifiedCount + " classes");
            setProgress(25 + (currentTransformer * 20));
        }
        
        // Записываем результат
        log("\n[4/4] Writing output JAR: " + outputPath);
        writeJar(outputPath, manifest, classes, resources, renamingTransformer);
        
        setProgress(90);
        
        log("\n========================================");
        log("     Obfuscation complete!");
        log("========================================");
        log("\nOutput: " + outputPath);
        
        setProgress(100);
        
        JOptionPane.showMessageDialog(this, "Obfuscation completed successfully!", 
            "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
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
                        ClassReader reader = new ClassReader(data);
                        ClassNode classNode = new ClassNode();
                        reader.accept(classNode, 0);
                        classes.put(classNode.name, classNode);
                    } else {
                        resources.put(name, data);
                    }
                }
            }
        }
        
        return manifest;
    }
    
    private void writeJar(String jarPath, Manifest manifest, Map<String, ClassNode> classes, 
                          Map<String, byte[]> resources, RenamingTransformer renamingTransformer) throws IOException {
        
        Path outputPath = Paths.get(jarPath);
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath), manifest)) {
            
            // Добавляем класс-дешифратор
            if (config.isStringEncryptionEnabled()) {
                ClassNode decryptorClass = StringEncryptionTransformer.createDecryptorClass();
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                decryptorClass.accept(cw);
                jos.putNextEntry(new JarEntry(decryptorClass.name + ".class"));
                jos.write(cw.toByteArray());
                jos.closeEntry();
            }
            
            // Добавляем обфусцированные классы
            for (ClassNode classNode : classes.values()) {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

                if (renamingTransformer != null) {
                    ClassRemapper remapper = new ClassRemapper(cw, renamingTransformer.getRemapper());
                    classNode.accept(remapper);
                } else {
                    classNode.accept(cw);
                }

                byte[] classData = cw.toByteArray();
                
                // ClassRemapper уже переименовал класс внутри cw, используем оригинальное имя
                String className = classNode.name;

                jos.putNextEntry(new JarEntry(className + ".class"));
                jos.write(classData);
                jos.closeEntry();
            }
            
            // Добавляем ресурсы
            for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
                String name = entry.getKey();
                
                if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA") || 
                    name.endsWith(".DSA") || name.equals("META-INF/MANIFEST.MF"))) {
                    continue;
                }
                
                jos.putNextEntry(new JarEntry(name));
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            ObfuscatorGUI gui = new ObfuscatorGUI();
            gui.setVisible(true);
        });
    }
}

