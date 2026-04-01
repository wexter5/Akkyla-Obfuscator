package com.obfuscator.util;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * Генератор фейковых классов для обфускации.
 * Создаёт классы, которые ломают декомпиляторы.
 */
public class DummyClassGenerator {
    
    /**
     * Генерирует фейковый класс, который выглядит как валидный для JVM,
     * но ломает декомпиляторы.
     */
    public static byte[] generateDummyClass(String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        // Создаём класс с ACC_SYNTHETIC и ACC_DEPRECATED
        cw.visit(V1_8, ACC_SYNTHETIC | ACC_DEPRECATED | ACC_FINAL, 
                 className, null, "java/lang/Object", null);
        
        // Добавляем фейковые поля
        for (int i = 0; i < 5; i++) {
            cw.visitField(ACC_SYNTHETIC | ACC_PRIVATE | ACC_STATIC, 
                         "field_" + i, "I", null, i).visitEnd();
        }
        
        // Добавляем конструктор
        MethodVisitor mv = cw.visitMethod(ACC_SYNTHETIC | ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
        // Добавляем фейковый статический блок с мусорным байт-кодом
        mv = cw.visitMethod(ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        
        // Мусорный код
        mv.visitInsn(ICONST_0);
        mv.visitInsn(POP);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(POP);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(POP);
        mv.visitInsn(NOP);
        mv.visitInsn(NOP);
        mv.visitInsn(NOP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 0);
        mv.visitEnd();
        
        // Добавляем фейковые методы
        for (int i = 0; i < 3; i++) {
            addDummyMethod(cw, className, "method_" + i, i);
        }
        
        // Добавляем метод с неправильным байт-кодом (ломает декомпиляторы)
        addCrashMethod(cw, className);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private static void addDummyMethod(ClassWriter cw, String className, String methodName, int index) {
        MethodVisitor mv = cw.visitMethod(
            ACC_SYNTHETIC | ACC_STATIC | ACC_FINAL, 
            methodName, 
            "(I)I", 
            null, 
            null
        );
        
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(ICONST_2);
        mv.visitInsn(IMUL);
        mv.visitInsn(IADD);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }
    
    private static void addCrashMethod(ClassWriter cw, String className) {
        // Метод с дублирующимися метками и сложным control flow
        MethodVisitor mv = cw.visitMethod(
            ACC_SYNTHETIC | ACC_PRIVATE | ACC_STATIC, 
            "verify", 
            "()Z", 
            null, 
            null
        );
        
        mv.visitCode();
        
        // Сложный control flow
        Label l1 = new Label();
        Label l2 = new Label();
        Label l3 = new Label();
        Label l4 = new Label();
        
        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ISTORE, 0);
        
        mv.visitLabel(l1);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitJumpInsn(IFEQ, l3);
        
        mv.visitLabel(l2);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 0);
        mv.visitJumpInsn(GOTO, l4);
        
        mv.visitLabel(l3);
        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ISTORE, 0);
        
        mv.visitLabel(l4);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(IRETURN);
        
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }
    
    /**
     * Генерирует класс с пустым телом методов (ломает JD-GUI).
     */
    public static byte[] generateEmptyClass(String className) {
        ClassWriter cw = new ClassWriter(0); // Без вычислений
        
        cw.visit(V1_8, ACC_SYNTHETIC | ACC_FINAL, className, null, "java/lang/Object", null);
        
        // Конструктор без кода
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitEnd();
        
        // Пустые методы
        for (int i = 0; i < 10; i++) {
            mv = cw.visitMethod(ACC_SYNTHETIC | ACC_STATIC, "m" + i, "()V", null, null);
            mv.visitEnd();
        }
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    /**
     * Генерирует класс с очень длинными именами методов.
     */
    public static byte[] generateLongNameClass(String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        
        cw.visit(V1_8, ACC_SYNTHETIC, className, null, "java/lang/Object", null);
        
        // Методы с очень длинными именами
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longName.append("IlIIlIlI");
        }
        
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, longName.toString(), "()V", null, null);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        cw.visitEnd();
        return cw.toByteArray();
    }
}
