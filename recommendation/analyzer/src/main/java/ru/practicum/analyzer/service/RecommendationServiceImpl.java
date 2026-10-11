package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserInteraction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserInteractionRepository;
import ru.practicum.stats.proto.dashboard.RecommendedEventProto;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int RECENT_LIMIT = 20;
    private static final int NEIGHBOURS_K = 10;

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    @Transactional(readOnly = true)
    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        // 1. Недавние взаимодействия пользователя
        List<UserInteraction> recent = userInteractionRepository.findRecentByUserId(
                userId, PageRequest.of(0, RECENT_LIMIT));

        if (recent.isEmpty()) {
            return Stream.empty();
        }

        Set<Long> viewedEventIds = new HashSet<>();
        recent.forEach(ui -> viewedEventIds.add(ui.getEventId()));

        // 2. Похожие на недавние, с которыми пользователь не взаимодействовал
        List<EventSimilarity> similarities = eventSimilarityRepository.findAllByEventIdsIn(
                recent.stream().map(UserInteraction::getEventId).toList());

        // Максимальный score для каждого кандидата
        Map<Long, Double> candidateScores = new HashMap<>();
        for (EventSimilarity es : similarities) {
            long candidate;
            if (viewedEventIds.contains(es.getEventA())) {
                candidate = es.getEventB();
            } else if (viewedEventIds.contains(es.getEventB())) {
                candidate = es.getEventA();
            } else {
                continue;
            }
            if (viewedEventIds.contains(candidate)) continue;

            candidateScores.merge(candidate, es.getScore(), Math::max);
        }

        // 3. Топ maxResults по score
        List<Long> topCandidates = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        // 4. Для всех кандидатов — предсказанная оценка (batch, без N+1)
        Map<Long, Double> predictedScores = predictScoresBatch(userId, topCandidates);

        return topCandidates.stream()
                .map(eventId -> RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(predictedScores.getOrDefault(eventId, 0.0))
                        .build());
    }

    /**
     * Batch-предсказание оценок для всех кандидатов.
     * Один запрос к БД за всеми соседями и один — за весами пользователя.
     */
    private Map<Long, Double> predictScoresBatch(long userId, List<Long> candidateEventIds) {
        if (candidateEventIds.isEmpty()) {
            return Map.of();
        }

        // 1. Для всех кандидатов — их соседи (batch)
        List<EventSimilarity> allNeighbours = eventSimilarityRepository.findAllByEventIdsIn(candidateEventIds);

        // candidate -> (neighbour -> score)
        Map<Long, Map<Long, Double>> neighboursByCandidate = new HashMap<>();
        Set<Long> allNeighbourIds = new HashSet<>();

        for (EventSimilarity es : allNeighbours) {
            long a = es.getEventA();
            long b = es.getEventB();
            if (candidateEventIds.contains(a)) {
                neighboursByCandidate.computeIfAbsent(a, k -> new HashMap<>()).merge(b, es.getScore(), Math::max);
                allNeighbourIds.add(b);
            }
            if (candidateEventIds.contains(b)) {
                neighboursByCandidate.computeIfAbsent(b, k -> new HashMap<>()).merge(a, es.getScore(), Math::max);
                allNeighbourIds.add(a);
            }
        }

        // 2. Один запрос — веса пользователя для всех соседей
        Map<Long, Double> userWeights = new HashMap<>();
        if (!allNeighbourIds.isEmpty()) {
            List<UserInteraction> interactions = userInteractionRepository
                    .findAllByUserIdAndEventIdIn(userId, allNeighbourIds.stream().toList());
            for (UserInteraction ui : interactions) {
                userWeights.put(ui.getEventId(), ui.getWeight());
            }
        }

        // 3. Считаем предсказание для каждого кандидата в памяти
        Map<Long, Double> result = new HashMap<>();
        for (Long candidate : candidateEventIds) {
            Map<Long, Double> neighbours = neighboursByCandidate.getOrDefault(candidate, Map.of());

            // Отбираем K ближайших соседей, с которыми пользователь взаимодействовал
            List<Map.Entry<Long, Double>> topNeighbours = neighbours.entrySet().stream()
                    .filter(e -> userWeights.containsKey(e.getKey()))
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(NEIGHBOURS_K)
                    .toList();

            if (topNeighbours.isEmpty()) {
                result.put(candidate, 0.0);
                continue;
            }

            double numerator = 0.0;
            double denominator = 0.0;
            for (Map.Entry<Long, Double> n : topNeighbours) {
                double sim = n.getValue();
                double weight = userWeights.get(n.getKey());
                numerator += sim * weight;
                denominator += sim;
            }

            result.put(candidate, denominator == 0.0 ? 0.0 : numerator / denominator);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        List<EventSimilarity> similarities = eventSimilarityRepository.findAllByEventId(eventId, PageRequest.of(0, 100));
        if (similarities.isEmpty()) return Stream.empty();

        // Собираем ID всех «других» мероприятий
        List<Long> otherEventIds = similarities.stream()
                .map(es -> (es.getEventA() == eventId) ? es.getEventB() : es.getEventA())
                .toList();

        // фильтр в БД — только те, с которыми пользователь взаимодействовал
        Set<Long> userEvents = userInteractionRepository
                .findEventIdsByUserIdAndEventIdIn(userId, otherEventIds);

        return similarities.stream()
                .map(es -> {
                    long other = (es.getEventA() == eventId) ? es.getEventB() : es.getEventA();
                    return Map.entry(other, es.getScore());
                })
                .filter(e -> !userEvents.contains(e.getKey()))
                .sorted(Comparator.comparingDouble((Map.Entry<Long, Double> e) -> e.getValue()).reversed())
                .limit(maxResults)
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Stream.empty();

        List<Object[]> results = userInteractionRepository.sumWeightsByEventIds(eventIds);
        return results.stream()
                .map(row -> RecommendedEventProto.newBuilder()
                        .setEventId((Long) row[0])
                        .setScore(((Number) row[1]).doubleValue())
                        .build());
    }
}