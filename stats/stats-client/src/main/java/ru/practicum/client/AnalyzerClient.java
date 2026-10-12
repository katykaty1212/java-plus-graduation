package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.proto.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.stats.proto.dashboard.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    /**
     * Получить рекомендации для пользователя.
     */
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            return asList(analyzerStub.getRecommendationsForUser(request));
        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций для пользователя {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Получить мероприятия, похожие на указанное, с которыми пользователь ещё не взаимодействовал.
     */
    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            return asList(analyzerStub.getSimilarEvents(request));
        } catch (Exception e) {
            log.error("Ошибка получения похожих событий для eventId={}: {}", eventId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Получить сумму взаимодействий для списка мероприятий.
     */
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto.Builder builder = InteractionsCountRequestProto.newBuilder();
            eventIds.forEach(builder::addEventId);
            return asList(analyzerStub.getInteractionsCount(builder.build()));
        } catch (Exception e) {
            log.error("Ошибка получения количества взаимодействий: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Преобразовать Iterator (результат gRPC-стрима) в Java List.
     */
    private List<RecommendedEventProto> asList(Iterator<RecommendedEventProto> iterator) {
        List<RecommendedEventProto> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }
}