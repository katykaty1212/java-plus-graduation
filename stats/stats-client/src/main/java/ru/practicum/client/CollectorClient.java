package ru.practicum.client;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.proto.collector.ActionTypeProto;
import ru.practicum.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.stats.proto.collector.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    /**
     * Отправить действие пользователя в Collector.
     *
     * @param userId     идентификатор пользователя
     * @param eventId    идентификатор мероприятия
     * @param actionType тип действия (VIEW, REGISTER, LIKE)
     */
    public void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {
        try {
            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();

            UserActionProto action = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(timestamp)
                    .build();

            collectorStub.collectUserAction(action);
            log.debug("Действие {} отправлено: userId={}, eventId={}", actionType, userId, eventId);
        } catch (Exception e) {
            // Collector не должен ломать основную операцию
            log.error("Не удалось отправить действие в Collector: {}", e.getMessage());
        }
    }
}