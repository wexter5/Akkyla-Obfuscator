package com.obfuscator.transformers;

import com.obfuscator.ObfuscatorConfig;
import com.obfuscator.Transformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

/**
 * Трансформер для шифрования строковых литералов
 * Использует XOR шифрование + Base64 кодирование
 */
public class StringEncryptionTransformer implements Transformer {
    
    private final ObfuscatorConfig config;
    private final Random random = new Random();
    
    // Класс для解密 строк (будет добавлен в JAR)
    private static final String DECRYPTOR_CLASS = "com/obfuscator/StringDecryptor";
    
    public StringEncryptionTransformer(ObfuscatorConfig config) {
        this.config = config;
    }
    
    @Override
    public String getName() {
        return "StringEncryptionTransformer";
    }
    
    @Override
    public boolean transform(ClassNode classNode, Map<String, ClassNode> classes) {
        boolean modified = false;
        
        // Пропускаем класс-дешифратор
        if (classNode.name.equals(DECRYPTOR_CLASS)) {
            return false;
        }
        
        for (MethodNode method : classNode.methods) {
            if (decryptStrings(method)) {
                modified = true;
            }
        }
        
        return modified;
    }
    
    /**
     * Замена всех строковых литералов на зашифрованные
     */
    private boolean decryptStrings(MethodNode method) {
        boolean modified = false;
        InsnList newInstructions = new InsnList();
        
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.LDC) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof String) {
                    String original = (String) ldc.cst;
                    
                    // Пропускаем короткие строки (имена полей, методы)
                    if (original.length() < 2) {
                        newInstructions.add(insn);
                        continue;
                    }
                    
                    // Шифруем строку
                    String encrypted = encryptString(original);
                    
                    // Заменяем LDC на вызов decrypt
                    newInstructions.add(new LdcInsnNode(encrypted));
                    newInstructions.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        DECRYPTOR_CLASS,
                        "decrypt",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        false
                    ));
                    
                    modified = true;
                    continue;
                }
            }
            newInstructions.add(insn);
        }
        
        if (modified) {
            method.instructions = newInstructions;
        }
        
        return modified;
    }
    
    /**
     * XOR шифрование + Base64
     */
    private String encryptString(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        byte key = (byte) (random.nextInt(254) + 1); // Ключ 1-254
        
        // XOR с ключом
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ key);
        }
        
        // Добавляем ключ в начало и кодируем Base64
        byte[] result = new byte[bytes.length + 1];
        result[0] = key;
        System.arraycopy(bytes, 0, result, 1, bytes.length);
        
        return Base64.getEncoder().encodeToString(result);
    }
    
    /**
     * Создать класс-дешифратор
     */
    public static ClassNode createDecryptorClass() {
        ClassNode classNode = new ClassNode();
        classNode.version = 52; // Java 8
        classNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
        classNode.name = DECRYPTOR_CLASS;
        classNode.superName = "java/lang/Object";
        
        // Конструктор
        MethodNode init = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        );
        init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        init.instructions.add(new InsnNode(Opcodes.RETURN));
        init.maxStack = 1;
        init.maxLocals = 1;
        classNode.methods.add(init);
        
        // Метод decrypt - упрощённая версия без try-catch
        MethodNode decrypt = new MethodNode(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "decrypt",
            "(Ljava/lang/String;)Ljava/lang/String;",
            null,
            null
        );
        
        // Локальные переменные
        LabelNode start = new LabelNode();
        LabelNode loopCondition = new LabelNode();
        LabelNode loopEnd = new LabelNode();
        
        decrypt.instructions.add(start);
        
        // Base64 decode - getDecoder() is static method
        decrypt.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;", false));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // Push the input string
        decrypt.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B", false));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ASTORE, 1)); // encryptedBytes
        
        // Получить ключ (первый байт)
        decrypt.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        decrypt.instructions.add(new InsnNode(Opcodes.ICONST_0));
        decrypt.instructions.add(new InsnNode(Opcodes.BALOAD));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ISTORE, 2)); // key
        
        // Создать массив для расшифрованных данных (без ключа)
        decrypt.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        decrypt.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        decrypt.instructions.add(new InsnNode(Opcodes.ICONST_1));
        decrypt.instructions.add(new InsnNode(Opcodes.ISUB));
        decrypt.instructions.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ASTORE, 3)); // decryptedBytes
        
        // Цикл XOR расшифровки
        decrypt.instructions.add(new InsnNode(Opcodes.ICONST_0));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ISTORE, 4)); // i = 0
        
        decrypt.instructions.add(loopCondition);
        decrypt.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        decrypt.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        decrypt.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, loopEnd));
        
        // Тело цикла: decryptedBytes[i] = (byte) (encryptedBytes[i+1] ^ key)
        decrypt.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        decrypt.instructions.add(new InsnNode(Opcodes.ICONST_1));
        decrypt.instructions.add(new InsnNode(Opcodes.IADD));
        decrypt.instructions.add(new InsnNode(Opcodes.BALOAD));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        decrypt.instructions.add(new InsnNode(Opcodes.IXOR));
        decrypt.instructions.add(new InsnNode(Opcodes.I2B));
        decrypt.instructions.add(new InsnNode(Opcodes.BASTORE));
        
        // i++
        decrypt.instructions.add(new IincInsnNode(4, 1));
        decrypt.instructions.add(new JumpInsnNode(Opcodes.GOTO, loopCondition));
        
        decrypt.instructions.add(loopEnd);
        
        // Создать String из decryptedBytes
        decrypt.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        decrypt.instructions.add(new InsnNode(Opcodes.DUP));
        decrypt.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        decrypt.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false));
        decrypt.instructions.add(new InsnNode(Opcodes.ARETURN));
        
        decrypt.maxStack = 4;
        decrypt.maxLocals = 5;
        
        classNode.methods.add(decrypt);
        
        return classNode;
    }
}
