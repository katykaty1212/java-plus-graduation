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

    // 1. Для Private API: Получить все события, созданные конкретным пользователем (с пагинацией)
    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    // 2. Для Private API: Найти конкретное событие по ID и проверить, что оно принадлежит автору
    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    // 3. Для Admin API (Справочно): Проверить, привязаны ли события к категории
    boolean existsByCategoryId(Long categoryId);

    // 4. Для Compilations (Справочно): Получить список событий по списку их ID
    List<Event> findAllByIdIn(List<Long> ids);

    // 5. Для Public API: Поиск ID опубликованных событий с фильтрами
//    @Query(value = "SELECT e.id FROM events e " +
//            "WHERE e.state = 'PUBLISHED' " +
//            "AND e.event_date >= :start AND e.event_date <= :end " +
//            "AND (CAST(:text AS VARCHAR) IS NULL OR " +
//            "     LOWER(e.annotation) LIKE LOWER(CONCAT('%', CAST(:text AS VARCHAR), '%')) OR " +
//            "     LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:text AS VARCHAR), '%'))) " +
//            "AND (CAST(:categories AS VARCHAR) IS NULL OR e.category_id IN (:categories)) " +
//            "AND (CAST(:paid AS VARCHAR) IS NULL OR e.paid = :paid) " +
//            "ORDER BY e.event_date ASC " +
//            "LIMIT :limit OFFSET :offset",
//            nativeQuery = true)
//    List<Long> findPublishedEventIdsWithFilters(
//            @Param("text") String text,
//            @Param("categories") List<Long> categories,
//            @Param("paid") Boolean paid,
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end,
//            @Param("limit") int limit,
//            @Param("offset") int offset);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = :state " +
            "AND e.eventDate BETWEEN :start AND :end " +
            "ORDER BY e.eventDate ASC")
    List<Event> findAllByStateAndEventDateBetween(
            @Param("state") EventState state,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 6. для Admin API: Поиск событий с фильтрами (для админа)
    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
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