package ru.practicum.aggregator.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityProducer {

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity}")
    private String eventsSimilarityTopic;

    public void send(EventSimilarityAvro msg) {
        kafkaTemplate.send(eventsSimilarityTopic, msg);
        log.debug("Отправлено сходство: A={}, B={}, score={}",
                msg.getEventA(), msg.getEventB(), msg.getScore());
    }
}