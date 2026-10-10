package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.proto.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.stats.proto.dashboard.UserPredictionsRequestProto;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    /**
     * Получить рекомендации для пользователя.
     */
    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            return asStream(analyzerStub.getRecommendationsForUser(request));
        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций для пользователя {}: {}", userId, e.getMessage());
            return Stream.empty();
        }
    }

    /**
     * Получить мероприятия, похожие на указанное, с которыми пользователь ещё не взаимодействовал.
     */
    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            return asStream(analyzerStub.getSimilarEvents(request));
        } catch (Exception e) {
            log.error("Ошибка получения похожих событий для eventId={}: {}", eventId, e.getMessage());
            return Stream.empty();
        }
    }

    /**
     * Получить сумму взаимодействий для списка мероприятий.
     */
    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto.Builder builder = InteractionsCountRequestProto.newBuilder();
            eventIds.forEach(builder::addEventId);
            return asStream(analyzerStub.getInteractionsCount(builder.build()));
        } catch (Exception e) {
            log.error("Ошибка получения количества взаимодействий: {}", e.getMessage());
            return Stream.empty();
        }
    }

    /**
     * Преобразовать Iterator (результат gRPC-стрима) в Java Stream.
     */
    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}