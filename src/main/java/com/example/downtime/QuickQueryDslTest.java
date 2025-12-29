package com.example.downtime;

import com.example.downtime.model.QDowntimeEvent;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

import java.time.LocalDateTime;

public class QuickQueryDslTest {
    public static void main(String[] args) {
        System.out.println("🚀 Быстрый тест QueryDSL метода");

        // 1. Создаем Q-класс вручную
        QDowntimeEvent q = QDowntimeEvent.downtimeEvent;

        // 2. Создаем предикат
        Predicate predicate = q.operatorId.eq("01");

        // 3. Создаем сортировку
        OrderSpecifier<LocalDateTime> order = q.startTime.desc();

        System.out.println("Предикат: " + predicate);
        System.out.println("Сортировка: " + order);
        System.out.println("Q-класс создан: " + (q != null));

        // Проверяем поля Q-класса
        System.out.println("\nПоля Q-класса:");
        System.out.println("operatorId field: " + q.operatorId);
        System.out.println("startTime field: " + q.startTime);
        System.out.println("id field: " + q.id);
    }
}
