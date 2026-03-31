# Akkyla Obfuscator

Обфускатор Java байт-кода на основе ASM с GUI интерфейсом.

![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## Возможности

| Трансформер | Описание | Статус |
|-------------|----------|--------|
| **Renaming** | Переименование классов, полей и методов в `a`, `b`, `c`... | ✅ |
| **String Encryption** | XOR шифрование строк + Base64 кодирование | ✅ |
| **Flow Obfuscation** | Добавление фиктивных инструкций для запутывания | ✅ |

## Быстрый старт

### GUI режим (рекомендуется)

```bash
java -jar target/java-obfuscator-1.0-SNAPSHOT.jar
```

Или просто дважды кликните по JAR файлу в проводнике.

### CLI режим

```bash
java -jar target/java-obfuscator-1.0-SNAPSHOT.jar -i input.jar -o output.jar
```

## Использование GUI

1. **Выберите входной JAR файл** — нажмите кнопку `...` рядом с полем "Input JAR"
2. **Выберите выходной файл** — заполнится автоматически или укажите вручную
3. **Настройте трансформеры**:
   - ☑ Renaming — переименование классов/полей/методов
   - ☑ String Encryption — шифрование строковых литералов
   - ☑ Flow Obfuscation — запутывание потока управления
   - Flow complexity — уровень запутывания (1-10)
4. **Нажмите "▶ Start Obfuscation"**
5. **Следите за прогрессом** в окне логов

![GUI Interface](docs/gui-screenshot.png)

## Опции CLI

| Опция | Описание |
|-------|----------|
| `-i, --input` | Входной JAR файл (обязательно) |
| `-o, --output` | Выходной JAR файл (обязательно) |
| `--no-rename` | Отключить переименование |
| `--no-string` | Отключить шифрование строк |
| `--no-flow` | Отключить flow obfuscation |
| `-gui` | Запустить GUI интерфейс |
| `-h, --help` | Показать справку |

### Примеры

```bash
# Полная обфускация
java -jar obfuscator.jar -i app.jar -o app-obf.jar

# Только шифрование строк
java -jar obfuscator.jar -i app.jar -o app-obf.jar --no-rename --no-flow

# Запуск GUI
java -jar obfuscator.jar -gui
```

## Сборка из исходников

### Требования
- Java 17+
- Maven 3.6+

```bash
cd obfuscator
mvn clean package
```

JAR файл будет создан в `target/java-obfuscator-1.0-SNAPSHOT.jar`

## Структура проекта

```
obfuscator/
├── src/main/java/com/obfuscator/
│   ├── Main.java                    # CLI интерфейс
│   ├── ObfuscatorConfig.java        # Конфигурация
│   ├── Transformer.java             # Интерфейс трансформера
│   └── transformers/
│       ├── RenamingTransformer.java      # Переименование
│       ├── StringEncryptionTransformer.java  # Шифрование строк
│       └── FlowObfuscationTransformer.java   # Flow obfuscation
├── src/main/java/com/obfuscator/ui/
│   └── ObfuscatorGUI.java           # GUI интерфейс
├── test/                            # Тестовые классы
├── pom.xml                          # Maven конфиг
└── README.md                        # Документация
```

## Как это работает

### 1. Renaming Transformer
- Сканирует все классы в JAR
- Создаёт маппинг имён (original -> obfuscated)
- Переименовывает классы, поля, методы используя ASM Remapper
- Сохраняет `main` метод и конструкторы без изменений

### 2. String Encryption Transformer
- Находит все `LDC` инструкции со строками
- Шифрует строки: XOR с случайным ключом + Base64
- Заменяет на вызов `StringDecryptor.decrypt()`
- Вставляет класс-дешифратор в JAR

### 3. Flow Obfuscation Transformer
- Добавляет `NOP` инструкции после каждой 5-й операции
- Усложняет статический анализ байт-кода
- Не влияет на выполнение программы

## Ограничения

- Не поддерживает классы с signature verification
- Может ломать reflection-зависимый код
- Flow obfuscation упрощён (NOP injection)

## Планы на будущее

- [ ] Control Flow Flattening (state machine)
- [ ] Dead code injection
- [ ] Arithmetic obfuscation
- [ ] Anti-decompiler tricks
- [ ] Конфигурация через YAML/JSON

## Лицензия

MIT License

## Авторы

Создано с помощью Qwen Code
