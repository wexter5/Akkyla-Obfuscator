# Minecraft Mod Obfuscator

Профессиональный JAR обфускатор для Fabric/Forge Minecraft модов с графическим интерфейсом.

## Возможности

### Модули обфускации

1. **String Encryption** (Шифрование строк)
   - Находит все LDC инструкции со строковыми константами
   - Шифрует строки используя XOR с динамическим ключом + Base64
   - Добавляет скрытый метод дешифровки в каждый класс
   - Уникальный ключ для каждой строки

2. **Control Flow Obfuscation** (Обфускация потока управления)
   - Opaque Predicates - условия, которые всегда истинны/ложны, но сложны для анализа
   - Code Jumbling - перемешивание независимых блоков кода
   - Вставка мусорных переходов и меток
   - NOP инструкции для усложнения анализа

3. **Decompiler Crasher** (Ломание декомпиляторов)
   - Удаление LineNumberTable и LocalVariableTable
   - Добавление SYNTHETIC флагов ко всем методам и полям
   - Jump into the middle of instructions (легально для JVM, сложно для декомпиляторов)
   - Дублирующиеся блоки мёртвого кода
   - Избыточные проверки null

### Совместимость с Fabric/Forge

- Автоматическое исключение Mixin классов
- Распознавание аннотаций @Inject, @ModifyConstant, @Redirect и других
- Исключение пакетов org.spongepowered.asm.mixin
- Сохранение работоспособности модов после обфускации

## Структура проекта

```
obfuscator/
├── pom.xml                          # Maven конфигурация
├── src/main/java/com/obfuscator/
│   ├── core/
│   │   ├── Transformer.java         # Интерфейс трансформера
│   │   ├── ObfuscationContext.java  # Контекст обфускации
│   │   ├── ObfuscationOptions.java  # Настройки обфускации
│   │   └── Obfuscator.java          # Главный класс обфускатора
│   ├── transformers/
│   │   ├── StringEncryptionTransformer.java    # Шифрование строк
│   │   ├── ControlFlowObfuscator.java          # Обфускация потока управления
│   │   └── DecompilerCrasherTransformer.java   # Ломание декомпиляторов
│   ├── gui/
│   │   └── ObfuscatorGUI.java       # Графический интерфейс
│   └── util/
│       ├── JarUtils.java            # Утилиты для работы с JAR
│       ├── MixinUtils.java          # Утилиты для Mixin
│       └── StringEncryptionUtils.java # Утилиты шифрования
└── README.md
```

## Сборка и запуск

### Требования
- Java 17 или выше
- Maven 3.6+

### Сборка

```bash
cd obfuscator
mvn clean package
```

Обфускатор будет создан в `target/minecraft-obfuscator-1.0.0-all.jar`

### Запуск

```bash
java -jar target/minecraft-obfuscator-1.0.0-all.jar
```

## Использование

1. Запустите обфускатор
2. Выберите входной JAR файл (ваш мод)
3. Выберите выходной JAR файл
4. Настройте опции обфускации:
   - String Encryption - шифрование строковых констант
   - Control Flow Obfuscation - обфускация потока управления
   - Decompiler Crasher - техники для ломания декомпиляторов
   - Exclude Mixin Classes - исключить Mixin классы (рекомендуется)
5. Добавьте пакеты/классы для исключения в список исключений
6. Нажмите "Process"

### Рекомендуемые исключения для Minecraft модов

```
com/yourmod/mixins/
org/spongepowered/asm/mixin/
net/minecraftforge/event/
net/fabricmc/fabric/api/
```

## Технические детали

### Алгоритм шифрования строк

```java
// Шифрование
encrypted[i] = (original[i] ^ (key + i))
result = Base64.encode(encrypted)

// Дешифрование (в рантайме)
decoded = Base64.decode(encrypted)
original[i] = (decoded[i] ^ (key + i))
```

### Используемые библиотеки

- **OW2 ASM 9.6** - манипуляция байт-кодом Java
- **Swing** - графический интерфейс

### Transformer Pattern

Каждый тип обфускации реализует интерфейс `Transformer`:

```java
public interface Transformer {
    boolean transform(ClassNode classNode, ObfuscationContext context);
    String getName();
    boolean shouldTransform(String className, ObfuscationContext context);
}
```

## Предупреждения

⚠️ **Важно:**
- Всегда тестируйте обфусцированный мод перед выпуском
- Некоторые анти-чит системы могут обнаруживать обфусцированные моды
- Не обфусцируйте классы с @Mod аннотациями
- Сохраняйте резервные копии оригинальных файлов

## Лицензия

MIT License - свободное использование и модификация.

## Авторы

Разработано для образовательных целей и защиты авторских модов.
