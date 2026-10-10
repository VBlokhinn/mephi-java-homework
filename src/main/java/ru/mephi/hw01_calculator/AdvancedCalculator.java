package ru.mephi.hw01_calculator;

import java.util.List;
import java.util.Scanner;

public class AdvancedCalculator {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        var operationList = List.of('+', '-', '*', '/');
        System.out.print("Введите первое число: ");
        double result = sc.nextDouble();
        while (true) {
            System.out.print("Введите операцию (+, -, *, /), C - сброс, S - результат: ");
            char operation = Character.toLowerCase(sc.next().charAt(0));
            if (operation == 's') {
                break;
            }
            if (operation == 'c') {
                System.out.print("Сброс! Введите новое первое число: ");
                result = sc.nextInt();
                continue;
            }
            if (!operationList.contains(operation)) {
                System.out.println("Неизвестная операция!");
                continue;
            }
            System.out.print("Введите следующее число: ");
            int nextOperand = sc.nextInt();
            switch (operation) {
                case '+' -> result += nextOperand;
                case '-' -> result -= nextOperand;
                case '*' -> result *= nextOperand;
                case '/' -> {
                    if (nextOperand == 0) {
                        System.out.println("Ошибка: деление на ноль!");
                    } else {
                        result /= nextOperand;
                    }
                }
            }
            System.out.println("Текущий результат: " + result);
        }
        System.out.println("Итоговый результат: " + result);
    }}