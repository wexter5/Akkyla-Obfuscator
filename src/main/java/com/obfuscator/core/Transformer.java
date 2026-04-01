package com.obfuscator.core;

import org.objectweb.asm.tree.ClassNode;

/**
 * Базовый интерфейс для всех трансформеров байт-кода.
 * Каждый тип обфускации реализуется как отдельный трансформер.
 */
public interface Transformer {
    
    /**
     * Трансформирует класс.
     * 
     * @param classNode ASM ClassNode для трансформации
     * @param context контекст обфускации
     * @return true если класс был изменён
     */
    boolean transform(ClassNode classNode, ObfuscationContext context);
    
    /**
     * Возвращает имя трансформера для логирования.
     */
    String getName();
    
    /**
     * Проверяет, должен ли этот трансформер применяться к классу.
     * 
     * @param className внутреннее имя класса (например, com/example/MyClass)
     * @param context контекст обфускации
     * @return true если трансформер должен обработать этот класс
     */
    default boolean shouldTransform(String className, ObfuscationContext context) {
        return !context.isExcluded(className);
    }
}
