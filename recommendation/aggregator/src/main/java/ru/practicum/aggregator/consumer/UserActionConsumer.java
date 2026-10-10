package ru.practicum.aggregator.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.aggregator.service.AggregatorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final AggregatorService aggregatorService;

    @KafkaListener(topics = "${kafka.topics.user-actions}", groupId = "aggregator")
    public void consume(UserActionAvro action) {
        log.info("Получено действие: userId={}, eventId={}, actionType={}",
                action.getUserId(), action.getEventId(), action.getActionType());
        aggregatorService.processUserAction(action);
    }
}