package com.obfuscator.transformers;

import com.obfuscator.core.ObfuscationContext;
import com.obfuscator.core.ObfuscationOptions;
import com.obfuscator.core.Transformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Трансформер для "ломания" декомпиляторов.
 * 
 * Использует легальные для JVM техники, которые сбивают с толку декомпиляторы:
 * - Удаление LineNumberTable и LocalVariableTable
 * - Добавление SYNTHETIC флагов
 * - Вставка NOP последовательностей
 * - Jump into the middle of instructions
 * - Duplicate code blocks
 * - Illegal (для декомпиляторов) bytecode sequences
 */
public class DecompilerCrasherTransformer implements Transformer, Opcodes {
    
    private final Random random = new Random();
    private int transformationsCount = 0;
    
    @Override
    public String getName() {
        return "DecompilerCrasher";
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
        ObfuscationOptions options = context.getOptions();
        
        // Добавляем SYNTHETIC флаги ко всем методам и полям
        if (options.isAddSyntheticFlags()) {
            changed |= addSyntheticFlags(classNode);
        }
        
        // Манипулируем LineNumberTable
        if (options.isRemoveLineNumberTable()) {
            changed |= removeLineNumberTables(classNode);
        }
        
        // Манипулируем LocalVariableTable
        if (options.isRemoveLocalVariableTable()) {
            changed |= removeLocalVariableTables(classNode);
        }
        
        // Вставляем NOP последовательности
        if (options.isInsertNops()) {
            changed |= insertNopSequences(classNode, context);
        }
        
        // Добавляем jump into the middle of instructions
        changed |= insertIllegalJumps(classNode, context);
        
        // Добавляем дублирующиеся блоки кода
        changed |= addDuplicateBlocks(classNode, context);
        
        if (changed) {
            transformationsCount++;
        }
        
        return changed;
    }
    
    /**
     * Добавляет ACC_SYNTHETIC флаг ко всем методам и полям.
     * Это заставляет декомпиляторы игнорировать их или отображать по-другому.
     */
    private boolean addSyntheticFlags(ClassNode classNode) {
        boolean changed = false;
        
        // Добавляем SYNTHETIC к полям
        for (FieldNode field : classNode.fields) {
            if ((field.access & ACC_SYNTHETIC) == 0) {
                field.access |= ACC_SYNTHETIC;
                changed = true;
            }
        }
        
        // Добавляем SYNTHETIC к методам (кроме публичных API)
        for (MethodNode method : classNode.methods) {
            if ((method.access & ACC_SYNTHETIC) == 0 &&
                (method.access & ACC_PUBLIC) == 0 &&
                !method.name.equals("<init>") &&
                !method.name.equals("<clinit>")) {
                method.access |= ACC_SYNTHETIC;
                changed = true;
            }
        }
        
        // Добавляем SYNTHETIC к самому классу
        if ((classNode.access & ACC_SYNTHETIC) == 0) {
            classNode.access |= ACC_SYNTHETIC;
            changed = true;
        }
        
        return changed;
    }
    
    /**
     * Удаляет LineNumberTable из всех методов.
     * Это усложняет отладку и анализ байт-кода.
     */
    private boolean removeLineNumberTables(ClassNode classNode) {
        boolean changed = false;
        
        for (MethodNode method : classNode.methods) {
            if (method.instructions != null) {
                InsnList newInstructions = new InsnList();
                
                for (AbstractInsnNode insn : method.instructions) {
                    if (!(insn instanceof LineNumberNode)) {
                        newInstructions.add(insn);
                    } else {
                        changed = true;
                    }
                }
                
                if (changed) {
                    method.instructions = newInstructions;
                }
            }
        }
        
        return changed;
    }
    
    /**
     * Удаляет LocalVariableTable из всех методов.
     * Это заставляет декомпиляторы использовать имена вроде var1, var2.
     */
    private boolean removeLocalVariableTables(ClassNode classNode) {
        boolean changed = false;
        
        for (MethodNode method : classNode.methods) {
            if (method.localVariables != null && !method.localVariables.isEmpty()) {
                method.localVariables.clear();
                changed = true;
            }
        }
        
        return changed;
    }
    
    /**
     * Вставляет длинные последовательности NOP инструкций.
     * Некоторые декомпиляторы не справляются с большим количеством NOP.
     */
    private boolean insertNopSequences(ClassNode classNode, ObfuscationContext context) {
        boolean changed = false;
        
        for (MethodNode method : classNode.methods) {
            if (method.instructions == null) continue;
            if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) continue;
            
            InsnList newInstructions = new InsnList();
            
            for (AbstractInsnNode insn : method.instructions) {
                newInstructions.add(insn);
                
                // Вставляем последовательность NOP после определённых инструкций
                if (insn.getOpcode() >= Opcodes.ILOAD && insn.getOpcode() <= Opcodes.ALOAD) {
                    if (context.getRandom().nextDouble() < 0.15) {
                        int nopCount = context.getRandom().nextInt(5) + 2;
                        for (int i = 0; i < nopCount; i++) {
                            newInstructions.add(new InsnNode(NOP));
                        }
                        changed = true;
                    }
                }
            }
            
            if (changed) {
                method.instructions = newInstructions;
            }
        }
        
        return changed;
    }
    
    /**
     * Вставляет "нелегальные" переходы - jump into the middle of instructions.
     * Это легально для JVM, но многие декомпиляторы не могут это обработать.
     */
    private boolean insertIllegalJumps(ClassNode classNode, ObfuscationContext context) {
        boolean changed = false;
        
        for (MethodNode method : classNode.methods) {
            if (method.instructions == null) continue;
            if ((method.access & (ACC_ABSTRACT | ACC_NATIVE | ACC_STATIC)) != 0) continue;
            if (method.name.equals("<clinit>")) continue;
            
            // Находим подходящие места для вставки
            List<AbstractInsnNode> targets = new ArrayList<>();
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == IINC || 
                    (insn.getOpcode() >= ISTORE && insn.getOpcode() <= ASTORE)) {
                    targets.add(insn);
                }
            }
            
            if (!targets.isEmpty() && context.getRandom().nextDouble() < 0.3) {
                AbstractInsnNode target = targets.get(context.getRandom().nextInt(targets.size()));
                
                // Создаём метку в середине инструкции
                LabelNode jumpTarget = new LabelNode(new Label());
                
                // Вставляем метку после целевой инструкции
                method.instructions.insert(target, jumpTarget);
                
                // Добавляем условный переход к этой метке
                InsnList jumpCode = new InsnList();
                
                // Создаём условие, которое всегда истинно
                jumpCode.add(new InsnNode(ICONST_1));
                jumpCode.add(new JumpInsnNode(IFNE, jumpTarget));
                
                // Вставляем перед какой-нибудь инструкцией
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn.getOpcode() == NOP) {
                        method.instructions.insert(insn, jumpCode);
                        changed = true;
                        break;
                    }
                }
            }
        }
        
        return changed;
    }
    
    /**
     * Добавляет дублирующиеся блоки кода, которые никогда не выполняются.
     * Это сбивает с толку анализ потока управления.
     */
    private boolean addDuplicateBlocks(ClassNode classNode, ObfuscationContext context) {
        boolean changed = false;
        
        for (MethodNode method : classNode.methods) {
            if (method.instructions == null) continue;
            if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) continue;
            if (method.maxStack < 2) continue;
            
            // Находим простые инструкции для дублирования
            List<AbstractInsnNode> duplicable = new ArrayList<>();
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == POP || insn.getOpcode() == POP2 ||
                    insn.getOpcode() == NOP || insn.getOpcode() == ACONST_NULL) {
                    duplicable.add(insn);
                }
            }
            
            if (duplicable.size() >= 2 && context.getRandom().nextDouble() < 0.4) {
                LabelNode skipLabel = new LabelNode(new Label());
                LabelNode continueLabel = new LabelNode(new Label());
                
                InsnList deadCode = new InsnList();
                
                // Условие, которое всегда ложно
                deadCode.add(new InsnNode(ICONST_0));
                deadCode.add(new JumpInsnNode(IFNE, skipLabel));
                
                // Дублируем инструкции
                for (int i = 0; i < 3; i++) {
                    AbstractInsnNode sample = duplicable.get(context.getRandom().nextInt(duplicable.size()));
                    deadCode.add(sample.clone(new HashMap<>()));
                }
                
                deadCode.add(new JumpInsnNode(GOTO, continueLabel));
                deadCode.add(skipLabel);
                
                // Ещё мёртвого кода
                for (int i = 0; i < 5; i++) {
                    AbstractInsnNode sample = duplicable.get(context.getRandom().nextInt(duplicable.size()));
                    deadCode.add(sample.clone(new HashMap<>()));
                }
                
                deadCode.add(continueLabel);
                
                // Вставляем в случайное место
                List<AbstractInsnNode> insertPoints = new ArrayList<>();
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof LabelNode) {
                        insertPoints.add(insn);
                    }
                }
                
                if (!insertPoints.isEmpty()) {
                    AbstractInsnNode insertPoint = insertPoints.get(
                        context.getRandom().nextInt(insertPoints.size())
                    );
                    method.instructions.insert(insertPoint, deadCode);
                    changed = true;
                }
            }
        }
        
        return changed;
    }
    
    /**
     * Добавляет избыточные проверки null, которые всегда истинны/ложны.
     */
    private boolean addRedundantNullChecks(ClassNode classNode, ObfuscationContext context) {
        boolean changed = false;
        
        for (MethodNode method : classNode.methods) {
            if (method.instructions == null) continue;
            
            InsnList newInstructions = new InsnList();
            
            for (AbstractInsnNode insn : method.instructions) {
                newInstructions.add(insn);
                
                if (insn.getOpcode() == ALOAD && context.getRandom().nextDouble() < 0.1) {
                    // Добавляем избыточную проверку
                    LabelNode skipLabel = new LabelNode(new Label());
                    
                    newInstructions.add(new InsnNode(DUP));
                    newInstructions.add(new JumpInsnNode(IFNULL, skipLabel));
                    newInstructions.add(new InsnNode(POP));
                    newInstructions.add(skipLabel);
                    
                    changed = true;
                }
            }
            
            if (changed) {
                method.instructions = newInstructions;
            }
        }
        
        return changed;
    }
    
    public int getTransformationsCount() {
        return transformationsCount;
    }
    
    public void resetCounter() {
        transformationsCount = 0;
    }
}
