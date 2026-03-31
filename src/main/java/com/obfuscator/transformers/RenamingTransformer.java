package com.obfuscator.transformers;

import com.obfuscator.ObfuscatorConfig;
import com.obfuscator.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Трансформер для переименования классов, полей и методов
 * Как Allatori - использует однобуквенные имена
 */
public class RenamingTransformer implements Transformer {
    
    private final ObfuscatorConfig config;
    private final Map<String, String> classMappings = new HashMap<>();
    private final Map<String, Map<String, String>> fieldMappings = new HashMap<>();
    private final Map<String, Map<String, String>> methodMappings = new HashMap<>();
    
    private Iterator<String> nameIterator;
    private final List<String> availableNames = new ArrayList<>();
    
    public RenamingTransformer(ObfuscatorConfig config) {
        this.config = config;
        initAvailableNames();
    }
    
    /**
     * Инициализация пула имён (a, b, c ... z, aa, ab, ac ...)
     */
    private void initAvailableNames() {
        // Однобуквенные имена
        for (char c = 'a'; c <= 'z'; c++) {
            availableNames.add(String.valueOf(c));
        }
        // Двухбуквенные имена
        for (char c1 = 'a'; c1 <= 'z'; c1++) {
            for (char c2 = 'a'; c2 <= 'z'; c2++) {
                availableNames.add("" + c1 + c2);
            }
        }
        // Трёхбуквенные и т.д.
        Collections.shuffle(availableNames);
        nameIterator = availableNames.iterator();
    }
    
    /**
     * Получить следующее доступное имя
     */
    private String getNextName() {
        if (!nameIterator.hasNext()) {
            // Если имена закончились, добавляем больше
            for (char c1 = 'a'; c1 <= 'z'; c1++) {
                for (char c2 = 'a'; c2 <= 'z'; c2++) {
                    for (char c3 = 'a'; c3 <= 'z'; c3++) {
                        availableNames.add("" + c1 + c2 + c3);
                    }
                }
            }
            Collections.shuffle(availableNames);
            nameIterator = availableNames.iterator();
        }
        return nameIterator.next();
    }
    
    @Override
    public String getName() {
        return "RenamingTransformer";
    }
    
    @Override
    public boolean transform(ClassNode classNode, Map<String, ClassNode> classes) {
        // Сначала собираем все классы для маппинга
        if (classMappings.isEmpty()) {
            buildClassMappings(classes);
        }
        
        // Применяем переименование к полям и методам
        renameFieldsAndMethods(classNode);
        
        return true;
    }
    
    /**
     * Построение маппинга классов
     */
    private void buildClassMappings(Map<String, ClassNode> classes) {
        System.out.println("[RenamingTransformer] Building class mappings...");
        
        for (ClassNode classNode : classes.values()) {
            // Пропускаем исключение и аннотации
            if (isExcluded(classNode.name) || classNode.name.startsWith("java/") || 
                classNode.name.startsWith("javax/") || classNode.name.startsWith("org/")) {
                continue;
            }
            
            String newName = getNextName();
            classMappings.put(classNode.name, newName);
            System.out.println("  " + classNode.name + " -> " + newName);
        }
    }
    
    /**
     * Переименование полей и методов
     */
    private void renameFieldsAndMethods(ClassNode classNode) {
        String className = classNode.name;
        
        if (isExcluded(className)) {
            return;
        }
        
        // Переименование полей
        Set<String> usedFieldNames = new HashSet<>();
        for (var field : classNode.fields) {
            if ((field.access & Opcodes.ACC_PUBLIC) != 0 && (field.access & Opcodes.ACC_STATIC) != 0) {
                // Пропускаем public static поля (часто это константы)
                continue;
            }
            if (isExcluded(className + "." + field.name)) {
                continue;
            }
            
            String newName = getUniqueName(usedFieldNames, className + ".field." + field.name);
            fieldMappings.computeIfAbsent(className, k -> new HashMap<>()).put(field.name, newName);
            usedFieldNames.add(newName);
        }
        
        // Переименование методов
        Set<String> usedMethodNames = new HashSet<>();
        usedMethodNames.add("<init>");
        usedMethodNames.add("<clinit>");
        usedMethodNames.add("main"); // main метод не трогаем
        
        for (var method : classNode.methods) {
            if (isExcluded(className + "." + method.name + method.desc)) {
                continue;
            }
            
            // Пропускаем конструкторы и статические инициализаторы
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                continue;
            }
            
            // Пропускаем main метод
            if (method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V")) {
                continue;
            }
            
            String newName = getUniqueName(usedMethodNames, className + ".method." + method.name + method.desc);
            methodMappings.computeIfAbsent(className, k -> new HashMap<>()).put(method.name + method.desc, newName);
            usedMethodNames.add(newName);
        }
    }
    
    /**
     * Получить уникальное имя для поля/метода
     */
    private String getUniqueName(Set<String> used, String key) {
        String name;
        do {
            name = getNextName();
        } while (used.contains(name));
        return name;
    }
    
    /**
     * Проверка на исключение
     */
    private boolean isExcluded(String name) {
        for (String exclude : config.getExcludeClasses()) {
            if (name.contains(exclude)) {
                return true;
            }
        }
        for (String exclude : config.getExcludeMethods()) {
            if (name.contains(exclude)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Получить Remapper для ClassRemapper
     */
    public Remapper getRemapper() {
        return new Remapper() {
            @Override
            public String map(String internalName) {
                return classMappings.getOrDefault(internalName, internalName);
            }
            
            @Override
            public String mapFieldName(String owner, String name, String desc) {
                Map<String, String> mappings = fieldMappings.get(owner);
                return mappings != null ? mappings.getOrDefault(name, name) : name;
            }
            
            @Override
            public String mapMethodName(String owner, String name, String desc) {
                Map<String, String> mappings = methodMappings.get(owner);
                return mappings != null ? mappings.getOrDefault(name + desc, name) : name;
            }
        };
    }
    
    public Map<String, String> getClassMappings() {
        return Collections.unmodifiableMap(classMappings);
    }
    
    public Map<String, Map<String, String>> getFieldMappings() {
        return Collections.unmodifiableMap(fieldMappings);
    }
    
    public Map<String, Map<String, String>> getMethodMappings() {
        return Collections.unmodifiableMap(methodMappings);
    }
}
