package ru.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.proto.collector.ActionTypeProto;
import ru.practicum.stats.proto.collector.UserActionProto;

import java.time.Instant;

@Component
public class UserActionMapper {

    public UserActionAvro toAvro(UserActionProto proto) {
        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(toAvroType(proto.getActionType()))
                .setTimestamp(toInstant(proto))
                .build();
    }

    private ActionTypeAvro toAvroType(ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + protoType);
        };
    }

    private Instant toInstant(UserActionProto proto) {
        return Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        );
    }
}