package ru.practicum.aggregator.state;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Хранит:
 * - веса пользователей по мероприятиям: Map<eventId, Map<userId, weight>>
 * - суммы весов мероприятий: Map<eventId, sumWeights>
 */
@Component
public class WeightsState {

    /** eventId -> (userId -> максимальный вес) */
    private final Map<Long, Map<Long, Double>> weights = new HashMap<>();

    /** eventId -> сумма весов */
    private final Map<Long, Double> sums = new HashMap<>();

    /**
     * Обновить вес пользователя для мероприятия.
     * @return {@link UpdateResult} с флагом changed и дельтой веса.
     *         Если changed=false — вес не изменился, дельта=0.
     */
    public synchronized UpdateResult updateWeight(long eventId, long userId, double newWeight) {
        Map<Long, Double> userWeights = weights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double oldWeight = userWeights.get(userId);

        if (oldWeight != null && oldWeight >= newWeight) {
            return new UpdateResult(false, 0.0, oldWeight);
        }

        double delta = (oldWeight == null) ? newWeight : (newWeight - oldWeight);
        userWeights.put(userId, newWeight);
        sums.merge(eventId, delta, Double::sum);
        return new UpdateResult(true, delta, oldWeight == null ? 0.0 : oldWeight);
    }

    public synchronized double getWeight(long eventId, long userId) {
        return weights.getOrDefault(eventId, Map.of()).getOrDefault(userId, 0.0);
    }

    public synchronized double getSum(long eventId) {
        return sums.getOrDefault(eventId, 0.0);
    }

    public synchronized Set<Long> getEventIds() {
        return new HashSet<>(weights.keySet());
    }

    /**
     * Вернуть мероприятия, с которыми пользователь уже взаимодействовал
     * (кроме указанного eventId).
     */
    public synchronized Set<Long> getEventsByUser(long userId, long excludeEventId) {
        Set<Long> result = new HashSet<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : weights.entrySet()) {
            long eventId = entry.getKey();
            if (eventId == excludeEventId) continue;
            if (entry.getValue().containsKey(userId)) {
                result.add(eventId);
            }
        }
        return result;
    }

    /**
     * Результат обновления веса.
     * @param changed — изменился ли вес
     * @param delta   — разница (new − old), 0 если changed=false
     * @param oldWeight — старое значение веса (0, если пользователь новый)
     */
    public record UpdateResult(boolean changed, double delta, double oldWeight) {
    }
}