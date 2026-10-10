package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.aggregator.producer.SimilarityProducer;
import ru.practicum.aggregator.state.MinWeightsSumsState;
import ru.practicum.aggregator.state.WeightsState;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private final WeightsState weightsState;
    private final MinWeightsSumsState minWeightsSumsState;
    private final SimilarityProducer similarityProducer;

    public void processUserAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventA = action.getEventId();
        double newWeight = toWeight(action.getActionType());

        // 1. Обновляем вес. Если не изменился — выходим.
        WeightsState.UpdateResult update = weightsState.updateWeight(eventA, userId, newWeight);
        if (!update.changed()) {
            log.debug("Вес не изменился: userId={}, eventId={}", userId, eventA);
            return;
        }

        double oldWeight = update.oldWeight();
        double delta = update.delta();

        // 2. Перебираем мероприятия, с которыми пользователь уже взаимодействовал
        //    (кроме eventA). С ними пересчитываем S_min и similarity.
        Set<Long> otherEvents = weightsState.getEventsByUser(userId, eventA);

        for (long eventB : otherEvents) {
            double wB = weightsState.getWeight(eventB, userId);

            // Старый вклад пользователя в S_min(A, B) — через старый вес A
            double oldContribution = Math.min(oldWeight, wB);
            // Новый вклад — через новый вес A
            double newContribution = Math.min(newWeight, wB);

            double deltaMin = newContribution - oldContribution;
            if (deltaMin != 0.0) {
                minWeightsSumsState.add(eventA, eventB, deltaMin);
            }

            // Пересчитываем similarity
            double score = calculateSimilarity(eventA, eventB);
            sendSimilarity(eventA, eventB, score, action.getTimestamp());
        }

        // Также надо обработать случай, когда для мероприятия A впервые появился пользователь,
        // но у него уже есть другие мероприятия — этот случай уже покрыт выше (otherEvents).

        // А если мероприятие A новое, и у пользователя нет других — цикл пустой,
        // ничего не отправляем. Это правильно.

        log.debug("Обработано действие: userId={}, eventA={}, delta={}, oldWeight={}",
                userId, eventA, delta, oldWeight);
    }

    private double calculateSimilarity(long eventA, long eventB) {
        double sMin = minWeightsSumsState.get(eventA, eventB);
        double sumA = weightsState.getSum(eventA);
        double sumB = weightsState.getSum(eventB);
        if (sumA == 0.0 || sumB == 0.0) {
            return 0.0;
        }
        return sMin / Math.sqrt(sumA * sumB);
    }

    private void sendSimilarity(long eventA, long eventB, double score, Instant timestamp) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        EventSimilarityAvro msg = EventSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();

        similarityProducer.send(msg);
    }

    private double toWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}