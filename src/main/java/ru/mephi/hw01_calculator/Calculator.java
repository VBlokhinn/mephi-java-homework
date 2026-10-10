package ru.mephi.hw01_calculator;

import java.util.List;
import java.util.Scanner;

public class Calculator {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Введите первое число: ");
        double firstOperand = sc.nextDouble();

        var operationList = List.of('+', '-', '*', '/');

        System.out.print("Введите операцию (+, -, *, /): ");
        char operation = sc.next().charAt(0);

        while (!operationList.contains(operation)) {
            System.out.println("Неизвестная операция!");
            System.out.print("Введите операцию (+, -, *, /): ");
            operation = sc.next().charAt(0);
        }
        System.out.print("Введите второе число: ");
        int nextOperand = sc.nextInt();
        double result = 0;
        switch (operation) {
            case '+' -> result = firstOperand + nextOperand;
            case '-' -> result = firstOperand - nextOperand;
            case '*' -> result = firstOperand * nextOperand;
            case '/' -> {
                if (nextOperand == 0) {
                    System.out.println("Ошибка: деление на ноль!");
                } else {
                    result = firstOperand / nextOperand;
                }
            }
        }
        System.out.print("Результат вычисления: " + result);
    }
}