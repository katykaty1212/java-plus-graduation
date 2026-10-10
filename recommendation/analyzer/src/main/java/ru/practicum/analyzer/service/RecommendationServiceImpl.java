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

        // 4. Для каждого кандидата — предсказанная оценка
        return topCandidates.stream()
                .map(eventId -> {
                    double predicted = predictScore(userId, eventId);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(predicted)
                            .build();
                });
    }

    /**
     * Предсказать оценку пользователя для мероприятия.
     * Берём K ближайших соседей (мероприятий пользователя) и считаем взвешенную оценку.
     */
    private double predictScore(long userId, long eventId) {
        // Похожие на eventId — все пары
        List<EventSimilarity> neighbours = eventSimilarityRepository.findAllByEventId(
                eventId, PageRequest.of(0, NEIGHBOURS_K));

        if (neighbours.isEmpty()) return 0.0;

        // Веса пользователя для этих соседей
        Set<Long> neighbourEventIds = new HashSet<>();
        Map<Long, Double> simByEvent = new HashMap<>();
        for (EventSimilarity es : neighbours) {
            long neighbourId = (es.getEventA() == eventId) ? es.getEventB() : es.getEventA();
            neighbourEventIds.add(neighbourId);
            simByEvent.merge(neighbourId, es.getScore(), Math::max);
        }

        List<UserInteraction> interactions = userInteractionRepository
                .findByUserIdAndEventIdIn(userId, neighbourEventIds.stream().toList());

        if (interactions.isEmpty()) return 0.0;

        double numerator = 0.0;
        double denominator = 0.0;
        for (UserInteraction ui : interactions) {
            Double sim = simByEvent.get(ui.getEventId());
            if (sim == null) continue;
            numerator += sim * ui.getWeight();
            denominator += sim;
        }
        if (denominator == 0.0) return 0.0;
        return numerator / denominator;
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        List<EventSimilarity> similarities = eventSimilarityRepository.findAllByEventId(eventId, PageRequest.of(0, 100));
        if (similarities.isEmpty()) return Stream.empty();

        Set<Long> userEvents = new HashSet<>(
                userInteractionRepository.findByUserId(userId).stream()
                        .map(UserInteraction::getEventId).toList());

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