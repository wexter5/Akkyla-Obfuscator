package com.obfuscator.transformers;

import com.obfuscator.core.ObfuscationContext;
import com.obfuscator.core.Transformer;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

/**
 * Ремепер классов - переименовывает ВСЕ классы.
 */
public class ClassRenamerTransformer implements Transformer {
    
    private int classCounter = 0;
    
    @Override
    public String getName() {
        return "ClassRenamer";
    }
    
    @Override
    public boolean shouldTransform(String className, ObfuscationContext context) {
        // Исключаем Mixin
        if (context.getOptions().isExcludeMixins()) {
            if (className.contains("mixin") || className.endsWith("Mixin")) {
                return false;
            }
        }
        // Исключаем из контекста (библиотеки и т.д.)
        if (context.isExcluded(className)) {
            return false;
        }
        return context.getOptions().isRenameClasses();
    }
    
    @Override
    public boolean transform(ClassNode classNode, ObfuscationContext context) {
        if (!shouldTransform(classNode.name, context)) {
            return false;
        }
        
        // Генерируем новое имя для класса
        String newName = "com/obf/" + generateClassName();
        
        // Сохраняем маппинг
        context.getClassMapping().put(classNode.name, newName);
        
        // Создаём ремепер и применяем
        ObfRemapper remapper = new ObfRemapper(context);
        
        // Ремепим через ASM Commons
        ClassNode newNode = new ClassNode();
        ClassRemapper cr = new ClassRemapper(newNode, remapper);
        classNode.accept(cr);
        
        // Копируем результат
        classNode.access = newNode.access;
        classNode.name = newName;  // Принудительно устанавливаем новое имя
        classNode.superName = newNode.superName;
        classNode.interfaces = newNode.interfaces;
        classNode.sourceFile = newNode.sourceFile;
        classNode.sourceDebug = newNode.sourceDebug;
        classNode.outerClass = newNode.outerClass;
        classNode.outerMethod = newNode.outerMethod;
        classNode.outerMethodDesc = newNode.outerMethodDesc;
        classNode.fields = newNode.fields;
        classNode.methods = newNode.methods;
        classNode.signature = newNode.signature;
        classNode.version = newNode.version;
        
        return true;
    }
    
    private String generateClassName() {
        int n = classCounter++;
        StringBuilder sb = new StringBuilder();
        do {
            sb.append((char)('A' + (n % 26)));
            n /= 26;
        } while (n > 0);
        return sb.toString();
    }
    
    static class ObfRemapper extends Remapper {
        private final ObfuscationContext context;
        private final Random random = new Random();
        
        public ObfRemapper(ObfuscationContext context) {
            this.context = context;
        }
        
        @Override
        public String map(String internalName) {
            // Возвращаем новое имя из маппинга или оригинал
            String mapped = context.getClassMapping().get(internalName);
            return mapped != null ? mapped : internalName;
        }
        
        @Override
        public String mapMethodName(String owner, String name, String desc) {
            if (name.equals("<init>") || name.equals("<clinit>")) {
                return name;
            }
            if (isExcluded(owner)) {
                return name;
            }
            
            String key = owner + "." + name + desc;
            String mapped = context.getMethodMapping().get(key);
            if (mapped == null) {
                mapped = "m" + context.getMethodMapping().size();
                context.getMethodMapping().put(key, mapped);
            }
            return mapped;
        }
        
        @Override
        public String mapFieldName(String owner, String name, String desc) {
            if (isExcluded(owner)) {
                return name;
            }
            
            String key = owner + "." + name + desc;
            String mapped = context.getFieldMapping().get(key);
            if (mapped == null) {
                mapped = "f" + context.getFieldMapping().size();
                context.getFieldMapping().put(key, mapped);
            }
            return mapped;
        }
        
        @Override
        public String mapDesc(String desc) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < desc.length(); i++) {
                char c = desc.charAt(i);
                if (c == 'L') {
                    int end = desc.indexOf(';', i);
                    if (end > i) {
                        String className = desc.substring(i + 1, end);
                        String mapped = context.getClassMapping().get(className);
                        if (mapped != null) {
                            result.append('L').append(mapped).append(';');
                            i = end;
                            continue;
                        }
                    }
                }
                result.append(c);
            }
            return result.toString();
        }
        
        @Override
        public String mapSignature(String signature, boolean typeSignature) {
            if (signature == null) return null;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < signature.length(); i++) {
                char c = signature.charAt(i);
                if (c == 'L') {
                    int end = signature.indexOf(';', i);
                    if (end > i) {
                        String className = signature.substring(i + 1, end);
                        String mapped = context.getClassMapping().get(className);
                        if (mapped != null) {
                            result.append('L').append(mapped).append(';');
                            i = end;
                            continue;
                        }
                    }
                }
                result.append(c);
            }
            return result.toString();
        }
        
        private boolean isExcluded(String className) {
            return className.startsWith("java/") ||
                   className.startsWith("javax/") ||
                   className.startsWith("kotlin/") ||
                   className.startsWith("org/lwjgl/") ||
                   className.startsWith("net/minecraft/") ||
                   className.startsWith("com/mojang/") ||
                   className.startsWith("org/spongepowered/");
        }
    }
}
