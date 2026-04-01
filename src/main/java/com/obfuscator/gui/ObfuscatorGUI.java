package com.obfuscator.gui;

import com.obfuscator.core.ObfuscationOptions;
import com.obfuscator.core.Obfuscator;
import com.obfuscator.util.FileDialogUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * Главное окно GUI обфускатора.
 */
public class ObfuscatorGUI extends JFrame implements Obfuscator.ProgressListener {
    
    private JTextField inputField;
    private JTextField outputField;
    private JTextArea exclusionArea;
    private JTextArea consoleArea;
    private JCheckBox stringEncryptionCheck;
    private JCheckBox controlFlowCheck;
    private JCheckBox decompilerCrasherCheck;
    private JCheckBox excludeMixinsCheck;
    private JCheckBox removeLineNumbersCheck;
    private JCheckBox removeLocalVarsCheck;
    private JCheckBox addSyntheticCheck;
    private JCheckBox insertNopsCheck;
    private JCheckBox renameClassesCheck;
    private JCheckBox addDummyClassesCheck;
    private JCheckBox importDummyClassesCheck;
    private JSpinner dummyClassCountSpinner;
    private JTextField dummyClassesFolderField;
    private JButton processButton;
    private JProgressBar progressBar;
    
    public ObfuscatorGUI() {
        initUI();
    }
    
    private void initUI() {
        setTitle("Minecraft Mod Obfuscator - Professional JAR Obfuscation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Верхняя панель - выбор файлов
        JPanel filePanel = createFilePanel();
        mainPanel.add(filePanel, BorderLayout.NORTH);
        
        // Центральная панель - опции и исключения
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        centerPanel.add(createOptionsPanel());
        centerPanel.add(createExclusionPanel());
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Нижняя панель - консоль и кнопка
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Input/Output JAR Files"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Input JAR
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Input JAR:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        inputField = new JTextField();
        inputField.setEditable(false);
        panel.add(inputField, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton inputBrowseButton = new JButton("Browse...");
        inputBrowseButton.addActionListener(e -> selectInputFile());
        panel.add(inputBrowseButton, gbc);
        
        // Output JAR
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Output JAR:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        outputField = new JTextField();
        outputField.setEditable(false);
        panel.add(outputField, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton outputBrowseButton = new JButton("Browse...");
        outputBrowseButton.addActionListener(e -> selectOutputFile());
        panel.add(outputBrowseButton, gbc);
        
        return panel;
    }
    
    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Obfuscation Options"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1;
        
        // String Encryption
        gbc.gridy = 0;
        stringEncryptionCheck = new JCheckBox("String Encryption", true);
        stringEncryptionCheck.setToolTipText("Encrypt string constants using XOR + Base64");
        panel.add(stringEncryptionCheck, gbc);
        
        // Control Flow Obfuscation
        gbc.gridy = 1;
        controlFlowCheck = new JCheckBox("Control Flow Obfuscation", true);
        controlFlowCheck.setToolTipText("Add opaque predicates and code jumbling");
        panel.add(controlFlowCheck, gbc);
        
        // Decompiler Crasher
        gbc.gridy = 2;
        decompilerCrasherCheck = new JCheckBox("Decompiler Crasher", true);
        decompilerCrasherCheck.setToolTipText("Add bytecode that confuses decompilers");
        panel.add(decompilerCrasherCheck, gbc);
        
        // Exclude Mixins
        gbc.gridy = 3;
        excludeMixinsCheck = new JCheckBox("Exclude Mixin Classes", true);
        excludeMixinsCheck.setToolTipText("Don't obfuscate Mixin classes (required for Forge/Fabric)");
        panel.add(excludeMixinsCheck, gbc);
        
        // Remove Line Numbers
        gbc.gridy = 4;
        removeLineNumbersCheck = new JCheckBox("Remove Line Number Tables", true);
        removeLineNumbersCheck.setToolTipText("Remove debugging information");
        panel.add(removeLineNumbersCheck, gbc);
        
        // Remove Local Variables
        gbc.gridy = 5;
        removeLocalVarsCheck = new JCheckBox("Remove Local Variable Tables", true);
        removeLocalVarsCheck.setToolTipText("Remove variable names from bytecode");
        panel.add(removeLocalVarsCheck, gbc);
        
        // Add Synthetic Flags
        gbc.gridy = 6;
        addSyntheticCheck = new JCheckBox("Add Synthetic Flags", true);
        addSyntheticCheck.setToolTipText("Mark methods/fields as synthetic");
        panel.add(addSyntheticCheck, gbc);
        
        // Insert NOPs
        gbc.gridy = 7;
        insertNopsCheck = new JCheckBox("Insert NOP Instructions", true);
        insertNopsCheck.setToolTipText("Add NOP instructions to confuse analysis");
        panel.add(insertNopsCheck, gbc);

        // Rename Classes (A-Z)
        gbc.gridy = 8;
        renameClassesCheck = new JCheckBox("Rename Classes (A-Z)", true);
        renameClassesCheck.setToolTipText("Rename classes to short names like A, B, C...");
        panel.add(renameClassesCheck, gbc);

        // Add Dummy Classes
        gbc.gridy = 9;
        addDummyClassesCheck = new JCheckBox("Add Dummy Classes", true);
        addDummyClassesCheck.setToolTipText("Add fake classes to confuse decompilers");
        panel.add(addDummyClassesCheck, gbc);

        // Dummy Class Count
        gbc.gridy = 10;
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        countPanel.add(new JLabel("Dummy class count:"));
        dummyClassCountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        dummyClassCountSpinner.setPreferredSize(new Dimension(60, 25));
        countPanel.add(dummyClassCountSpinner);
        panel.add(countPanel, gbc);

        // Import Dummy Classes from folder
        gbc.gridy = 11;
        importDummyClassesCheck = new JCheckBox("Import dummy classes from folder", false);
        importDummyClassesCheck.setToolTipText("Load custom dummy classes from a folder");
        panel.add(importDummyClassesCheck, gbc);

        // Dummy Classes Folder
        gbc.gridy = 12;
        JPanel folderPanel = new JPanel(new GridBagLayout());
        GridBagConstraints fbc = new GridBagConstraints();
        fbc.insets = new Insets(2, 5, 2, 5);
        fbc.fill = GridBagConstraints.HORIZONTAL;
        
        fbc.gridx = 0;
        fbc.weightx = 0;
        folderPanel.add(new JLabel("Folder:"), fbc);
        
        fbc.gridx = 1;
        fbc.weightx = 1;
        dummyClassesFolderField = new JTextField();
        dummyClassesFolderField.setEditable(false);
        dummyClassesFolderField.setColumns(15);
        folderPanel.add(dummyClassesFolderField, fbc);
        
        fbc.gridx = 2;
        fbc.weightx = 0;
        JButton browseFolderButton = new JButton("...");
        browseFolderButton.setPreferredSize(new Dimension(30, 25));
        browseFolderButton.addActionListener(e -> selectDummyClassesFolder());
        folderPanel.add(browseFolderButton, fbc);
        
        panel.add(folderPanel, gbc);

        return panel;
    }
    
    private JPanel createExclusionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Exclusion List"));
        
        JLabel label = new JLabel("Classes/Packages to exclude (one per line):");
        panel.add(label, BorderLayout.NORTH);
        
        exclusionArea = new JTextArea(10, 25);
        exclusionArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        exclusionArea.setText(
            "com/example/mod/mixins/\n" +
            "org/spongepowered/asm/mixin/\n" +
            "net/minecraftforge/event/\n" +
            "com/example/mod/api/"
        );
        
        JScrollPane scrollPane = new JScrollPane(exclusionArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        // Консоль
        consoleArea = new JTextArea(8, 50);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        consoleArea.setEditable(false);
        consoleArea.setBackground(new Color(30, 30, 30));
        consoleArea.setForeground(new Color(0, 255, 0));
        consoleArea.setCaretColor(Color.WHITE);
        
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setBorder(new TitledBorder("Process Console"));
        panel.add(consoleScrollPane, BorderLayout.CENTER);
        
        // Правая панель с кнопкой и прогрессом
        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Кнопка Process
        gbc.gridx = 0;
        gbc.gridy = 0;
        processButton = new JButton("▶ Process");
        processButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        processButton.setPreferredSize(new Dimension(150, 40));
        processButton.addActionListener(e -> startObfuscation());
        rightPanel.add(processButton, gbc);
        
        // Progress Bar
        gbc.gridy = 1;
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(150, 25));
        rightPanel.add(progressBar, gbc);
        
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private void selectInputFile() {
        File selected = FileDialogUtil.showOpenDialog(
            this, 
            "Выберите JAR файл для обфускации"
        );
        
        if (selected != null) {
            inputField.setText(selected.getAbsolutePath());

            // Автозаполнение output если пустой
            if (outputField.getText().isEmpty()) {
                String outputPath = selected.getParentFile().getAbsolutePath() +
                    File.separator + "obfuscated_" + selected.getName();
                outputField.setText(outputPath);
            }
        }
    }

    private void selectOutputFile() {
        File selected = FileDialogUtil.showSaveDialog(
            this, 
            "Сохранить обфусцированный JAR как"
        );

        if (selected != null) {
            outputField.setText(selected.getAbsolutePath());
        }
    }

    private void selectDummyClassesFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите папку с dummy классами");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            dummyClassesFolderField.setText(selected.getAbsolutePath());
        }
    }
    
    private void startObfuscation() {
        String inputPath = inputField.getText().trim();
        String outputPath = outputField.getText().trim();
        
        if (inputPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select an input JAR file.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (outputPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select an output JAR file.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        File inputJar = new File(inputPath);
        if (!inputJar.exists()) {
            JOptionPane.showMessageDialog(this,
                "Input file does not exist: " + inputPath,
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Блокируем кнопку
        processButton.setEnabled(false);
        consoleArea.setText("");
        progressBar.setValue(0);
        
        // Создаём опции
        ObfuscationOptions options = new ObfuscationOptions();
        options.setStringEncryption(stringEncryptionCheck.isSelected());
        options.setControlFlowObfuscation(controlFlowCheck.isSelected());
        options.setDecompilerCrasher(decompilerCrasherCheck.isSelected());
        options.setExcludeMixins(excludeMixinsCheck.isSelected());
        options.setRemoveLineNumberTable(removeLineNumbersCheck.isSelected());
        options.setRemoveLocalVariableTable(removeLocalVarsCheck.isSelected());
        options.setAddSyntheticFlags(addSyntheticCheck.isSelected());
        options.setInsertNops(insertNopsCheck.isSelected());
        options.setRenameClasses(renameClassesCheck.isSelected());
        options.setAddDummyClasses(addDummyClassesCheck.isSelected());
        options.setDummyClassCount((Integer) dummyClassCountSpinner.getValue());
        options.setImportDummyClasses(importDummyClassesCheck.isSelected());
        options.setDummyClassesFolder(dummyClassesFolderField.getText().trim());

        // Запускаем в отдельном потоке
        Thread obfuscationThread = new Thread(() -> {
            try {
                Obfuscator obfuscator = new Obfuscator(options, this);

                // Добавляем исключения
                String[] exclusions = exclusionArea.getText().split("\n");
                for (String exclusion : exclusions) {
                    exclusion = exclusion.trim();
                    if (!exclusion.isEmpty()) {
                        obfuscator.getContext().addExclusionPattern(exclusion);
                    }
                }

                obfuscator.obfuscate(inputJar, new File(outputPath));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "Error during obfuscation: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    processButton.setEnabled(true);
                });
            }
        });
        
        obfuscationThread.start();
    }
    
    @Override
    public void onProgress(int current, int total, String message) {
        SwingUtilities.invokeLater(() -> {
            int percent = (int) ((current * 100.0) / total);
            progressBar.setValue(percent);
            progressBar.setString(current + " / " + total + " (" + percent + "%)");
            
            // Добавляем в консоль (ограничиваем вывод)
            String shortMessage = message;
            if (shortMessage.length() > 80) {
                shortMessage = "..." + shortMessage.substring(shortMessage.length() - 77);
            }
            appendToConsole("[" + current + "/" + total + "] " + shortMessage);
        });
    }
    
    @Override
    public void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            appendToConsole("ERROR: " + error);
        });
    }
    
    @Override
    public void onComplete(String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(100);
            progressBar.setString("Complete!");
            appendToConsole("========================================");
            appendToConsole(message);
            appendToConsole("========================================");
            processButton.setEnabled(true);
            
            JOptionPane.showMessageDialog(this,
                message,
                "Obfuscation Complete",
                JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    private void appendToConsole(String message) {
        consoleArea.append(message + "\n");
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
    }
    
    /**
     * Точка входа приложения.
     */
    public static void main(String[] args) {
        // Устанавливаем Look and Feel
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
