package ru.practicum.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {

    private final EventSimilarityRepository eventSimilarityRepository;

    @KafkaListener(topics = "${kafka.topics.events-similarity}", groupId = "analyzer")
    @Transactional
    public void consume(EventSimilarityAvro msg) {
        eventSimilarityRepository.findByEventAAndEventB(msg.getEventA(), msg.getEventB())
                .ifPresentOrElse(
                        existing -> {
                            existing.setScore(msg.getScore());
                            existing.setUpdatedAt(toLocalDateTime(msg.getTimestamp()));
                            eventSimilarityRepository.save(existing);
                        },
                        () -> {
                            EventSimilarity es = EventSimilarity.builder()
                                    .eventA(msg.getEventA())
                                    .eventB(msg.getEventB())
                                    .score(msg.getScore())
                                    .updatedAt(toLocalDateTime(msg.getTimestamp()))
                                    .build();
                            eventSimilarityRepository.save(es);
                        }
                );

        log.debug("Обновлено сходство: A={}, B={}, score={}",
                msg.getEventA(), msg.getEventB(), msg.getScore());
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}