package ru.practicum.analyzer.service;

import ru.practicum.stats.proto.dashboard.RecommendedEventProto;

import java.util.List;
import java.util.stream.Stream;

public interface RecommendationService {

    Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults);

    Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults);

    Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds);
}