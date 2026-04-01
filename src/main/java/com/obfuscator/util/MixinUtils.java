package com.obfuscator.util;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

/**
 * Утилиты для работы с Mixin аннотациями и определения Mixin классов.
 */
public class MixinUtils {
    
    private static final String[] MIXIN_ANNOTATIONS = {
        "Lorg/spongepowered/asm/mixin/Mixin;",
        "Lorg/spongepowered/asm/mixin/Shadow;",
        "Lorg/spongepowered/asm/mixin/Overwrite;",
        "Lorg/spongepowered/asm/mixin/Inject;",
        "Lorg/spongepowered/asm/mixin/Redirect;",
        "Lorg/spongepowered/asm/mixin/ModifyConstant;",
        "Lorg/spongepowered/asm/mixin/ModifyArg;",
        "Lorg/spongepowered/asm/mixin/ModifyArgs;",
        "Lorg/spongepowered/asm/mixin/ModifyVariable;",
        "Lorg/spongepowered/asm/mixin/Unique;",
        "Lorg/spongepowered/asm/mixin/Pseudo;",
        "Lorg/spongepowered/asm/mixin/gen/Accessor;",
        "Lorg/spongepowered/asm/mixin/gen/Invoker;"
    };
    
    private static final String[] MIXIN_PACKAGES = {
        "org/spongepowered/asm/mixin/",
        "net/fabricmc/fabric/mixin/",
        "net/minecraftforge/fml/common/Mod/"
    };
    
    /**
     * Проверяет, является ли класс Mixin классом.
     */
    public static boolean isMixinClass(ClassNode classNode) {
        // Проверяем имя класса
        if (classNode.name.endsWith("Mixin")) {
            return true;
        }
        
        // Проверяем наличие Mixin аннотаций
        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : classNode.visibleAnnotations) {
                if (isMixinAnnotation(annotation.desc)) {
                    return true;
                }
            }
        }
        
        // Проверяем аннотации методов и полей
        if (hasMixinAnnotations(classNode)) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isMixinAnnotation(String desc) {
        for (String mixinAnnotation : MIXIN_ANNOTATIONS) {
            if (desc.contains(mixinAnnotation.replace(";", ""))) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean hasMixinAnnotations(ClassNode classNode) {
        // Проверяем методы
        if (classNode.methods != null) {
            for (var method : classNode.methods) {
                if (method.visibleAnnotations != null) {
                    for (AnnotationNode annotation : method.visibleAnnotations) {
                        if (isMixinAnnotation(annotation.desc)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        // Проверяем поля
        if (classNode.fields != null) {
            for (var field : classNode.fields) {
                if (field.visibleAnnotations != null) {
                    for (AnnotationNode annotation : field.visibleAnnotations) {
                        if (isMixinAnnotation(annotation.desc)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Проверяет, находится ли класс в пакете Mixin.
     */
    public static boolean isInMixinPackage(String className) {
        for (String pkg : MIXIN_PACKAGES) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        
        // Проверяем на наличие "mixin" в пути
        String[] parts = className.split("/");
        for (String part : parts) {
            if (part.toLowerCase().contains("mixin")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Проверяет, содержит ли класс аннотации Forge/Fabric.
     */
    public static boolean hasModAnnotation(ClassNode classNode) {
        if (classNode.visibleAnnotations == null) {
            return false;
        }
        
        String[] modAnnotations = {
            "Lnet/minecraftforge/fml/common/Mod;",
            "Lnet/fabricmc/api/ModInitializer;",
            "Lnet/fabricmc/api/ClientModInitializer;",
            "Lnet/fabricmc/fabric/api/client/modinitializer/ClientModInitializer;",
            "Lnet/fabricmc/fabric/api/modinitializer/v1/ModInitializer;"
        };
        
        for (AnnotationNode annotation : classNode.visibleAnnotations) {
            for (String modAnnotation : modAnnotations) {
                if (annotation.desc.contains(modAnnotation.replace(";", ""))) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
