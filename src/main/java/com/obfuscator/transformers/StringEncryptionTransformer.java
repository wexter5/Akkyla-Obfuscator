package com.obfuscator.transformers;

import com.obfuscator.core.ObfuscationContext;
import com.obfuscator.core.Transformer;
import com.obfuscator.util.StringEncryptionUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.ASM9;

import java.util.*;

/**
 * Трансформер для шифрования строковых констант.
 * Версия 2 - исправленная.
 */
public class StringEncryptionTransformer implements Transformer, Opcodes {
    
    private static final String DECRYPT_METHOD_NAME = "decodeString";
    private static final String DECRYPT_METHOD_DESC = "(Ljava/lang/String;)Ljava/lang/String;";
    private static final String DECRYPT_METHOD_SIG = "(Ljava/lang/String;)Ljava/lang/String;";
    
    private int encryptedStringCount = 0;
    
    @Override
    public String getName() {
        return "StringEncryption";
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
        Map<AbstractInsnNode, String> stringLiterals = new HashMap<>();
        
        for (MethodNode method : classNode.methods) {
            if (method.instructions == null) continue;
            if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) continue;
            
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == LDC) {
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    if (ldc.cst instanceof String) {
                        String str = (String) ldc.cst;
                        if (str.length() > 1 && !str.startsWith("(") && !str.startsWith("L") && !str.startsWith("[")) {
                            stringLiterals.put(insn, str);
                        }
                    }
                }
            }
        }
        
        if (!stringLiterals.isEmpty()) {
            addDecryptMethod(classNode);
            
            for (MethodNode method : classNode.methods) {
                if (method.instructions == null) continue;
                if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) continue;
                
                List<AbstractInsnNode> toReplace = new ArrayList<>();
                for (AbstractInsnNode insn : method.instructions) {
                    if (stringLiterals.containsKey(insn)) {
                        toReplace.add(insn);
                    }
                }
                
                for (AbstractInsnNode insn : toReplace) {
                    String original = stringLiterals.get(insn);
                    replaceLdcWithDecrypt(classNode, method, insn, original);
                    changed = true;
                    encryptedStringCount++;
                }
            }
        }
        
        return changed;
    }
    
    private void addDecryptMethod(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if (DECRYPT_METHOD_NAME.equals(method.name) && DECRYPT_METHOD_DESC.equals(method.desc)) {
                return;
            }
        }
        
        MethodNode decryptMethod = new MethodNode(
            ASM9,
            ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
            DECRYPT_METHOD_NAME,
            DECRYPT_METHOD_DESC,
            null,
            null
        );
        
        // Максимально простой байт-код:
        // ALOAD 0
        // INVOKESTATIC java/util/Base64.getDecoder()Ljava/util/Base64$Decoder;
        // ALOAD 0
        // INVOKEINTERFACE java/util/Base64$Decoder.decode(Ljava/lang/String;)[B
        // LDC "UTF-8"
        // INVOKESPECIAL java/lang/String.<init>([BLjava/lang/String;)V
        // ARETURN
        
        decryptMethod.instructions.add(new VarInsnNode(ALOAD, 0));
        decryptMethod.instructions.add(new MethodInsnNode(
            INVOKESTATIC,
            "java/util/Base64",
            "getDecoder",
            "()Ljava/util/Base64$Decoder;",
            false
        ));
        decryptMethod.instructions.add(new VarInsnNode(ALOAD, 0));
        decryptMethod.instructions.add(new MethodInsnNode(
            INVOKEINTERFACE,
            "java/util/Base64$Decoder",
            "decode",
            "(Ljava/lang/String;)[B",
            true
        ));
        decryptMethod.instructions.add(new LdcInsnNode("UTF-8"));
        decryptMethod.instructions.add(new MethodInsnNode(
            INVOKESPECIAL,
            "java/lang/String",
            "<init>",
            "([BLjava/lang/String;)V",
            false
        ));
        decryptMethod.instructions.add(new InsnNode(ARETURN));
        
        // Не устанавливаем maxStack/maxLocals - ClassWriter.COMPUTE_FRAMES сделает это
        
        classNode.methods.add(decryptMethod);
    }
    
    private void replaceLdcWithDecrypt(ClassNode classNode, MethodNode method, AbstractInsnNode ldcNode, String original) {
        InsnList replacement = new InsnList();
        
        String encrypted = StringEncryptionUtils.encrypt(original, 0);
        
        replacement.add(new LdcInsnNode(encrypted));
        replacement.add(new MethodInsnNode(
            INVOKESTATIC,
            classNode.name,
            DECRYPT_METHOD_NAME,
            DECRYPT_METHOD_DESC,
            false
        ));
        
        method.instructions.insert(ldcNode, replacement);
        method.instructions.remove(ldcNode);
    }
    
    public int getEncryptedStringCount() {
        return encryptedStringCount;
    }
}
