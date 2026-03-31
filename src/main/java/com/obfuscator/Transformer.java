package com.obfuscator;

import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

/**
 * Базовый интерфейс для всех трансформеров
 */
public interface Transformer {
    
    /**
     * Название трансформера
     */
    String getName();
    
    /**
     * Преобразует класс
     * @param classNode ASM ClassNode для преобразования
     * @param classes Карта всех классов для контекста
     * @return true если класс был изменён
     */
    boolean transform(ClassNode classNode, Map<String, ClassNode> classes);
}
