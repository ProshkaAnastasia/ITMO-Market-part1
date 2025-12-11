#!/bin/bash

# Скрипт для рекурсивного копирования содержимого всех файлов в директории в один файл
# Использование: ./copy_files_script.sh <исходная_директория> <выходной_файл>

SOURCE_DIR="${1:-.}"
OUTPUT_FILE="${2:-output.txt}"

# Проверка, что исходная директория существует
if [ ! -d "$SOURCE_DIR" ]; then
    echo "Ошибка: директория '$SOURCE_DIR' не найдена"
    exit 1
fi

# Очистка выходного файла, если он существует
> "$OUTPUT_FILE"

# Функция для рекурсивного обхода директории
process_directory() {
    local dir="$1"
    local indent="$2"
    
    # Обход всех элементов в директории
    for item in "$dir"/*; do
        # Пропуск, если элемент не существует (например, если директория пуста)
        [ -e "$item" ] || continue
        
        local basename="${item##*/}"
        local relative_path="${item#./}"
        
        if [ -f "$item" ]; then
            # Это файл - выводим название и содержимое
            echo "========================================" >> "$OUTPUT_FILE"
            echo "FILE: $relative_path" >> "$OUTPUT_FILE"
            echo "========================================" >> "$OUTPUT_FILE"
            cat "$item" >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
            
            echo "Обработан файл: $relative_path"
            
        elif [ -d "$item" ]; then
            # Это директория - рекурсивно обходим её
            echo "Обхождение директории: $relative_path"
            process_directory "$item" "$((indent + 1))"
        fi
    done
}

# Запуск процесса
echo "Начало сбора файлов из директории: $SOURCE_DIR"
echo "Результат будет сохранен в: $OUTPUT_FILE"
echo ""

process_directory "$SOURCE_DIR"

echo ""
echo "Готово! Все файлы скопированы в: $OUTPUT_FILE"
echo "Размер файла: $(du -h "$OUTPUT_FILE" | cut -f1)"
