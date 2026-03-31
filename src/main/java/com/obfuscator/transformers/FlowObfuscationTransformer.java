package com.obfuscator.transformers;

import com.obfuscator.ObfuscatorConfig;
import com.obfuscator.Transformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Flow Obfuscation Transformer
 * Запутывает поток управления методом преобразования в state machine
 * Как Allatori - использует switch/case с фиктивными переходами
 */
public class FlowObfuscationTransformer implements Transformer {
    
    private final ObfuscatorConfig config;
    private final Random random = new Random();
    
    public FlowObfuscationTransformer(ObfuscatorConfig config) {
        this.config = config;
    }
    
    @Override
    public String getName() {
        return "FlowObfuscationTransformer";
    }
    
    @Override
    public boolean transform(ClassNode classNode, Map<String, ClassNode> classes) {
        boolean modified = false;
        
        for (MethodNode method : classNode.methods) {
            // Пропускаем слишком короткие методы
            if (method.instructions.size() < 10) {
                continue;
            }
            // Пропускаем конструкторы и статические инициализаторы
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                continue;
            }
            
            if (obfuscateFlow(method)) {
                modified = true;
            }
        }
        
        return modified;
    }
    
    /**
     * Преобразование потока управления
     * Простая версия - добавляет фиктивные инструкции
     */
    private boolean obfuscateFlow(MethodNode method) {
        InsnList newInstructions = new InsnList();
        int nopCount = 0;
        int insnCount = 0;
        
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            newInstructions.add(insn);
            
            // Добавляем NOP после каждой 5-й инструкции
            if (insn.getOpcode() != -1 && insnCount % 5 == 4 && nopCount < config.getFlowComplexity()) {
                newInstructions.add(new InsnNode(Opcodes.NOP));
                nopCount++;
            }
            insnCount++;
        }

        if (nopCount > 0) {
            method.instructions = newInstructions;
            return true;
        }
        return false;
    }
}
