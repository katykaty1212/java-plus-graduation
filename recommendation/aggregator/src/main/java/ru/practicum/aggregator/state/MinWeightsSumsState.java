package ru.practicum.aggregator.state;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Хранит S_min(A, B) для пар мероприятий.
 * Ключи упорядочены: first = min(A,B), second = max(A,B).
 */
@Component
public class MinWeightsSumsState {

    /** first -> (second -> S_min) */
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    public synchronized void put(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    public synchronized double get(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums
                .getOrDefault(first, Map.of())
                .getOrDefault(second, 0.0);
    }

    public synchronized void add(long eventA, long eventB, double delta) {
        put(eventA, eventB, get(eventA, eventB) + delta);
    }
}