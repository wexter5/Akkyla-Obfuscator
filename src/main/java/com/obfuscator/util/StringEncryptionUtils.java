package com.obfuscator.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Утилиты для шифрования строк.
 * Использует комбинацию XOR и Base64 для обфускации строковых констант.
 */
public class StringEncryptionUtils {
    
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    
    /**
     * Шифрует строку используя XOR с динамическим ключом и Base64 кодирование.
     * 
     * @param original оригинальная строка
     * @param key ключ шифрования
     * @return зашифрованная строка в Base64
     */
    public static String encrypt(String original, int key) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = new byte[bytes.length];
        
        // XOR шифрование с динамическим ключом
        for (int i = 0; i < bytes.length; i++) {
            encrypted[i] = (byte) (bytes[i] ^ (key + i));
        }
        
        return BASE64_ENCODER.encodeToString(encrypted);
    }
    
    /**
     * Дешифрует строку.
     * 
     * @param encrypted зашифрованная строка в Base64
     * @param key ключ шифрования
     * @return оригинальная строка
     */
    public static String decrypt(String encrypted, int key) {
        if (encrypted == null || encrypted.isEmpty()) {
            return encrypted;
        }
        
        byte[] decoded = BASE64_DECODER.decode(encrypted);
        byte[] decrypted = new byte[decoded.length];
        
        // XOR дешифрование (та же операция что и шифрование)
        for (int i = 0; i < decoded.length; i++) {
            decrypted[i] = (byte) (decoded[i] ^ (key + i));
        }
        
        return new String(decrypted, StandardCharsets.UTF_8);
    }
    
    /**
     * Генерирует случайный ключ для шифрования.
     */
    public static int generateKey() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }
    
    /**
     * Создаёт уникальный ключ на основе индекса и сидa.
     */
    public static int generateKey(int index, int seed) {
        return seed ^ (index * 31) ^ (index >>> 2);
    }
}
