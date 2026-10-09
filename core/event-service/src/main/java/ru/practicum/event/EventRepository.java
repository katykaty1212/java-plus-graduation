package ru.practicum.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    // 1. Для Private API: получить все события, созданные конкретным пользователем (с пагинацией)
    // initiatorId — просто Long, а не объект User (user-service теперь отдельный микросервис)
    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    // 2. Для Private API: найти конкретное событие по ID и проверить, что оно принадлежит автору
    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    // 3. Для Admin API: проверить, привязаны ли события к категории
    boolean existsByCategoryId(Long categoryId);

    // 4. Для Compilations: получить список событий по списку их ID
    List<Event> findAllByIdIn(List<Long> ids);

    // 5. Для Public API: поиск опубликованных событий в диапазоне дат
    @Query("SELECT e FROM Event e " +
            "WHERE e.state = :state " +
            "AND e.eventDate BETWEEN :start AND :end " +
            "ORDER BY e.eventDate ASC")
    List<Event> findAllByStateAndEventDateBetween(
            @Param("state") EventState state,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 6. Для Admin API: поиск событий с фильтрами
    // ВАЖНО: было e.initiator.id — стало e.initiatorId, т.к. Event.initiator теперь Long
    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiatorId IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findEventsByAdminFilters(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);
}