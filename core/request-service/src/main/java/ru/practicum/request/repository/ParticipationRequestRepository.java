package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.RequestStatus;

import java.util.List;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    // 1. Получить все заявки, которые подал конкретный пользователь (на чужие события)
    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    // 2. Получить все заявки, поданные на конкретное событие
    List<ParticipationRequest> findAllByEventId(Long eventId);

    // 3. Проверить, подавал ли уже пользователь заявку на это событие
    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // 4. Посчитать количество заявок на событие с определенным статусом
    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    // 5. Получить список заявок по их ID и ID события
    List<ParticipationRequest> findAllByEventIdAndIdIn(Long eventId, List<Long> ids);

    @Query("SELECT r.eventId, COUNT(r) FROM ParticipationRequest r " +
            "WHERE r.eventId IN :eventIds AND r.status = :status " +
            "GROUP BY r.eventId")
    List<Object[]> countByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
                                            @Param("status") RequestStatus status);

    // 6. Проверка «есть ли заявка с таким статусом»
    boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, RequestStatus status);
}