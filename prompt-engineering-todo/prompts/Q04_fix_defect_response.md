# Ответ модели на запрос Q04 (Исправление дефекта D01)
Дата: 2026-11-23
Модель: Gemini (Web version)

## Причина сбоя:
В методе parseIndex использовалось нестрогое сравнение верхней границы `id >= size` вместо строгого `id > size`. При обращении к последней задаче (`id == size`) условие срабатывало ошибочно.

## Исправленный метод:
```java
    private static Integer parseIndex(String line, int cmdLength, int size) {
        String arg = line.length() > cmdLength ? line.substring(cmdLength).trim() : "";
        if (arg.isEmpty()) {
            System.out.println("ERROR: INVALID_ID");
            return null;
        }
        int id;
        try {
            id = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: INVALID_ID");
            return null;
        }
        if (id < 1 || id > size) {
            System.out.println("ERROR: NOT_FOUND");
            return null;
        }
        return id - 1;
    }
```
