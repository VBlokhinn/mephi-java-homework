# Матрица прослеживаемости требований (Traceability Matrix)

| ID | Формулировка проверяемого требования | Сценарии проверки | Фактический статус | Файл с подтверждением |
| :--- | :--- | :--- | :--- | :--- |
| **R01** | Файл `TodoListApp.java`, без `package`, Java 17 (UTF-8), стандартная библиотека, 50–150 строк | V01 (Компиляция), V02 (Просмотр) | **Пройдено** | `evidence/build.log` (106 строк, exit 0) |
| **R02** | Номера 1..N, статусы [ ]/[X], сдвиг нумерации при удалении | T01, T05, T06, T07 | **Пройдено** | `evidence/run_T01.log`, `run_T05.log`, `run_T06.log`, `run_T07.log` |
| **R03** | `add <текст>`, обрезка крайних пробелов, вывод `ADDED <id>`, ошибка `ERROR: EMPTY_TASK` | T01, T03, T04 | **Пройдено** | `evidence/run_T01.log`, `run_T03.log`, `run_T04.log` |
| **R04** | `list` построчно `<id>. [ ] <текст>`, вывод `EMPTY` при пустом списке | T01, T02, T03, T04, T05, T06, T07, T12 | **Пройдено** | `evidence/run_T01.log`, `run_T02.log`, `run_T12.log` |
| **R05** | `done <id>`: перевод в выполненные, `DONE <id>`, при повторе `ALREADY_DONE <id>` | T05, defect test | **Пройдено** | `evidence/run_T05.log`, `evidence/defect_D01_after.log` |
| **R06** | `remove <id>`: удаление, `REMOVED <id>`, перенумерация списка | T06, T07 | **Пройдено** | `evidence/run_T06.log`, `evidence/run_T07.log` |
| **R07** | Некорректный ID в `done`/`remove`: `ERROR: INVALID_ID`, вне диапазона: `ERROR: NOT_FOUND` | T08, T09 | **Пройдено** | `evidence/run_T08.log`, `evidence/run_T09.log` |
| **R08** | Регистр команд, игнорирование пустых строк, `ERROR: UNKNOWN_COMMAND` | T10, T11, T12 | **Пройдено** | `evidence/run_T10.log`, `evidence/run_T11.log`, `evidence/run_T12.log` |
| **R09** | Завершение по `exit` (`BYE`) или EOF (молча) | T01, T02, T12 | **Пройдено** | `evidence/run_T01.log`, `evidence/run_T02.log`, `evidence/run_T12.log` |
| **R10** | Отсутствие приглашений ввода (`> `), строго одна строка на действие (кроме `list`) | T01–T12, V02 | **Пройдено** | `evidence/run_T01.log` – `evidence/run_T12.log` |
