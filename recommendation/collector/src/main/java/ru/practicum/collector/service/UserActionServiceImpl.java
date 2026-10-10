package ru.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.practicum.stats.proto.collector.UserActionProto;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;
    private final UserActionMapper mapper;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;

    @Override
    public void collectUserAction(UserActionProto action) {
        UserActionAvro avro = mapper.toAvro(action);
        kafkaTemplate.send(userActionsTopic, avro);
        log.info("Отправлено действие: userId={}, eventId={}, actionType={}",
                avro.getUserId(), avro.getEventId(), avro.getActionType());
    }
}