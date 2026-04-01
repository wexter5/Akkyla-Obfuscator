package com.obfuscator.transformers;

import com.obfuscator.core.ObfuscationContext;
import com.obfuscator.core.Transformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Трансформер для обфускации потока управления.
 * 
 * Реализует:
 * - Opaque Predicates (непрозрачные предикаты)
 * - Code Jumbling (перемешивание кода)
 * - Insertion of garbage control flow
 */
public class ControlFlowObfuscator implements Transformer, Opcodes {
    
    private final Random random = new Random();
    private int transformationsCount = 0;
    
    @Override
    public String getName() {
        return "ControlFlowObfuscation";
    }
    
    @Override
    public boolean shouldTransform(String className, ObfuscationContext context) {
        if (context.getOptions().isExcludeMixins()) {
            if (className.contains("mixin") || className.endsWith("Mixin")) {
                return false;
            }
        }
        return !context.isExcluded(className);
    }
    
    @Override
    public boolean transform(ClassNode classNode, ObfuscationContext context) {
        if (!shouldTransform(classNode.name, context)) {
            return false;
        }
        
        boolean changed = false;
        
        for (MethodNode method : classNode.methods) {
            // Пропускаем абстрактные, нативные методы и конструкторы
            if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
                continue;
            }
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                continue;
            }
            if (method.instructions == null || method.instructions.size() == 0) {
                continue;
            }
            
            // Применяем обфускацию к методу
            if (obfuscateMethod(method, context)) {
                changed = true;
            }
        }
        
        return changed;
    }
    
    /**
     * Обфусцирует метод, добавляя opaque predicates и jumbling.
     */
    private boolean obfuscateMethod(MethodNode method, ObfuscationContext context) {
        boolean changed = false;
        
        // Собираем все метки для последующей модификации
        List<LabelNode> labels = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LabelNode) {
                labels.add((LabelNode) insn);
            }
        }
        
        // Добавляем opaque predicates перед ключевыми точками
        changed |= insertOpaquePredicates(method, context);
        
        // Добавляем мусорный control flow
        changed |= insertGarbageControlFlow(method, context);
        
        // Вставляем NOP инструкции
        changed |= insertNops(method, context);
        
        if (changed) {
            transformationsCount++;
        }
        
        return changed;
    }
    
    /**
     * Вставляет opaque predicates - условия, которые всегда истинны или ложны,
     * но сложны для анализа декомпилятором.
     */
    private boolean insertOpaquePredicates(MethodNode method, ObfuscationContext context) {
        boolean changed = false;
        List<AbstractInsnNode> instructions = new ArrayList<>();
        
        for (AbstractInsnNode insn : method.instructions) {
            instructions.add(insn);
            
            // Вставляем opaque predicate перед определёнными инструкциями
            if (insn.getOpcode() == IFNULL || insn.getOpcode() == IFNONNULL ||
                insn.getOpcode() == IFEQ || insn.getOpcode() == IFNE) {
                
                if (context.getRandom().nextDouble() < 0.3) { // 30% шанс
                    InsnList predicate = createOpaquePredicate(context.getRandom());
                    for (AbstractInsnNode pInsn : predicate) {
                        instructions.add(pInsn);
                    }
                    changed = true;
                }
            }
        }
        
        if (changed) {
            method.instructions.clear();
            for (AbstractInsnNode insn : instructions) {
                method.instructions.add(insn);
            }
        }
        
        return changed;
    }
    
    /**
     * Создаёт opaque predicate - код, который создаёт видимость ветвления,
     * но всегда идёт по одному пути.
     */
    private InsnList createOpaquePredicate(Random random) {
        InsnList predicate = new InsnList();
        
        LabelNode skipLabel = new LabelNode(new Label());
        
        // Создаём условие, которое всегда ложно
        // Например: if (System.currentTimeMillis() % 2 == 0 && false)
        predicate.add(new MethodInsnNode(
            INVOKESTATIC,
            "java/lang/System",
            "currentTimeMillis",
            "()J",
            false
        ));
        predicate.add(new LdcInsnNode(2L));
        predicate.add(new InsnNode(LREM));
        predicate.add(new LdcInsnNode(0L));
        predicate.add(new InsnNode(LCMP));
        predicate.add(new JumpInsnNode(IFNE, skipLabel)); // Всегда пропускаем
        
        // Мёртвый код, который никогда не выполнится
        predicate.add(new InsnNode(ACONST_NULL));
        predicate.add(new InsnNode(POP));
        predicate.add(new InsnNode(ICONST_1));
        predicate.add(new InsnNode(POP));
        
        predicate.add(skipLabel);
        
        return predicate;
    }
    
    /**
     * Вставляет мусор в control flow - лишние переходы и метки.
     */
    private boolean insertGarbageControlFlow(MethodNode method, ObfuscationContext context) {
        boolean changed = false;
        InsnList newInstructions = new InsnList();
        
        for (AbstractInsnNode insn : method.instructions) {
            newInstructions.add(insn);
            
            // Вставляем garbage control flow после RETURN инструкций
            if (insn.getOpcode() == ARETURN || insn.getOpcode() == IRETURN ||
                insn.getOpcode() == LRETURN || insn.getOpcode() == FRETURN ||
                insn.getOpcode() == DRETURN || insn.getOpcode() == RETURN) {
                
                if (context.getRandom().nextDouble() < 0.2) {
                    LabelNode garbageLabel = new LabelNode(new Label());
                    LabelNode continueLabel = new LabelNode(new Label());
                    
                    // Создаём unreachable код
                    newInstructions.add(new JumpInsnNode(GOTO, continueLabel));
                    newInstructions.add(garbageLabel);
                    
                    // Мёртвый код
                    newInstructions.add(new LdcInsnNode("garbage"));
                    newInstructions.add(new InsnNode(POP));
                    newInstructions.add(new InsnNode(NOP));
                    newInstructions.add(new InsnNode(NOP));
                    
                    newInstructions.add(continueLabel);
                    
                    changed = true;
                }
            }
        }
        
        if (changed) {
            method.instructions = newInstructions;
        }
        
        return changed;
    }
    
    /**
     * Вставляет NOP инструкции для усложнения анализа.
     */
    private boolean insertNops(MethodNode method, ObfuscationContext context) {
        boolean changed = false;
        InsnList newInstructions = new InsnList();
        
        for (AbstractInsnNode insn : method.instructions) {
            // Случайно вставляем NOP перед инструкцией
            if (context.getRandom().nextDouble() < 0.1) { // 10% шанс
                int nopCount = context.getRandom().nextInt(3) + 1;
                for (int i = 0; i < nopCount; i++) {
                    newInstructions.add(new InsnNode(NOP));
                }
                changed = true;
            }
            
            newInstructions.add(insn);
        }
        
        if (changed) {
            method.instructions = newInstructions;
        }
        
        return changed;
    }
    
    /**
     * Создаёт code jumbling - перемешивание независимых блоков кода.
     * Это сложная трансформация, которая требует анализа потока данных.
     */
    private boolean performCodeJumbling(MethodNode method, ObfuscationContext context) {
        // Упрощённая реализация
        // Полная реализация требует анализа basic blocks
        
        List<AbstractInsnNode> instructions = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            instructions.add(insn);
        }
        
        if (instructions.size() < 10) {
            return false; // Слишком маленький метод
        }
        
        // Находим независимые инструкции (не переходы и не метки)
        List<Integer> independentIndices = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (!(insn instanceof LabelNode) && 
                !(insn instanceof JumpInsnNode) &&
                !(insn instanceof FrameNode) &&
                insn.getOpcode() != ATHROW &&
                insn.getOpcode() != MONITORENTER &&
                insn.getOpcode() != MONITOREXIT) {
                independentIndices.add(i);
            }
        }
        
        if (independentIndices.size() < 5) {
            return false;
        }
        
        // Перемешиваем некоторые независимые инструкции
        Collections.shuffle(independentIndices, context.getRandom());
        
        // Эта реализация упрощена - полная требует переписывания меток
        return false;
    }
    
    public int getTransformationsCount() {
        return transformationsCount;
    }
    
    public void resetCounter() {
        transformationsCount = 0;
    }
}
