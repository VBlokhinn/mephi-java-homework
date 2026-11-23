# Тестовые сценарии (T01–T12) и матрица покрытия
Версия: 1.0 (утверждена на Шаге B)

## Уточнение по пустой строке:
Строки, состоящие только из пробельных символов (`line.trim().isEmpty()`), считаются пустыми строками и молча игнорируются.

## Спецификация сценариев

### T01. Позитивный (базовый цикл: добавление и просмотр)
- Требования: R02, R03, R04, R09
- Вход:
add Купить хлеб
add Позвонить маме
list
exit
- Ожидаемый вывод:
ADDED 1
ADDED 2
1. [ ] Купить хлеб
2. [ ] Позвонить маме
BYE

### T02. Граничный (пустой список при запуске)
- Требования: R04, R09
- Вход:
list
exit
- Ожидаемый вывод:
EMPTY
BYE

### T03. Позитивный (обрезка крайних пробелов и сохранение внутренних)
- Требования: R03
- Вход:
add    Выучить   Java 17   
list
exit
- Ожидаемый вывод:
ADDED 1
1. [ ] Выучить   Java 17
BYE

### T04. Негативный / Граничный (пустой текст задачи и текст только из пробелов)
- Требования: R03
- Вход:
add
add 
add     
list
exit
- Ожидаемый вывод:
ERROR: EMPTY_TASK
ERROR: EMPTY_TASK
ERROR: EMPTY_TASK
EMPTY
BYE

### T05. Позитивный и граничный (выполнение задачи и повторное выполнение)
- Требования: R02, R04, R05
- Вход:
add Сделать зарядку
done 1
list
done 1
exit
- Ожидаемый вывод:
ADDED 1
DONE 1
1. [X] Сделать зарядку
ALREADY_DONE 1
BYE

### T06. Позитивный (удаление из середины списка и сдвиг нумерации)
- Требования: R02, R04, R06
- Вход:
add Первая
add Вторая
add Третья
remove 2
list
exit
- Ожидаемый вывод:
ADDED 1
ADDED 2
ADDED 3
REMOVED 2
1. [ ] Первая
2. [ ] Третья
BYE

### T07. Граничный (удаление единственной задачи — возврат к пустому списку и сброс счетчика)
- Требования: R02, R04, R06
- Вход:
add Единственная
remove 1
list
add Новая
list
exit
- Ожидаемый вывод:
ADDED 1
REMOVED 1
EMPTY
ADDED 1
1. [ ] Новая
BYE

### T08. Негативный (отсутствующий или ненатуральный ID в done и remove)
- Требования: R07
- Вход:
add Задача
done
done abc
done 1.5
remove
remove --1
exit
- Ожидаемый вывод:
ADDED 1
ERROR: INVALID_ID
ERROR: INVALID_ID
ERROR: INVALID_ID
ERROR: INVALID_ID
ERROR: INVALID_ID
BYE

### T09. Граничный (выход за нижнюю и верхнюю границы диапазона 1..N, включая пустой список)
- Требования: R07
- Вход:
done 1
add Задача 1
add Задача 2
done 0
remove -1
done 3
remove 3
exit
- Ожидаемый вывод:
ERROR: NOT_FOUND
ADDED 1
ADDED 2
ERROR: NOT_FOUND
ERROR: NOT_FOUND
ERROR: NOT_FOUND
ERROR: NOT_FOUND
BYE

### T10. Негативный (чувствительность к регистру и неизвестные команды)
- Требования: R08
- Вход:
ADD Тест
List
DONE 1
REMOVE 1
EXIT
delete 1
exit
- Ожидаемый вывод:
ERROR: UNKNOWN_COMMAND
ERROR: UNKNOWN_COMMAND
ERROR: UNKNOWN_COMMAND
ERROR: UNKNOWN_COMMAND
ERROR: UNKNOWN_COMMAND
ERROR: UNKNOWN_COMMAND
BYE

### T11. Негативный (лишние аргументы у команд list и exit)
- Требования: R08
- Вход:
list all
list 1
exit 0
exit now
exit
- Ожидаемый вывод:
ERROR: UNKNOWN_COMMAND
ERROR: UNKNOWN_COMMAND
ERROR: UNKNOWN_COMMAND
ERROR: UNKNOWN_COMMAND
BYE

### T12. Граничный (игнорирование пустых строк и завершение по EOF без вывода BYE)
- Требования: R08, R09, R10
- Вход:

add Задача

list
- Ожидаемый вывод:
ADDED 1
1. [ ] Задача
