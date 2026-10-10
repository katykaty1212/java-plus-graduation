package ru.practicum.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserInteraction;

import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    /** Последние N взаимодействий пользователя (для рекомендаций). */
    @Query("SELECT ui FROM UserInteraction ui WHERE ui.userId = :userId ORDER BY ui.lastActionAt DESC")
    List<UserInteraction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /** Все взаимодействия пользователя (для фильтрации просмотренного). */
    List<UserInteraction> findByUserId(Long userId);

    /** Все взаимодействия пользователя с указанными мероприятиями. */
    List<UserInteraction> findByUserIdAndEventIdIn(Long userId, List<Long> eventIds);

    /** Сумма весов по мероприятиям. */
    @Query("SELECT ui.eventId, SUM(ui.weight) FROM UserInteraction ui WHERE ui.eventId IN :eventIds GROUP BY ui.eventId")
    List<Object[]> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);
}