package ru.practicum.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;

import java.util.List;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    // 1. Получить все заявки, которые подал конкретный пользователь (на чужие события)
    // Используется в: GET /users/{userId}/requests
    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    // 2. Получить все заявки, поданные на конкретное событие
    // Используется в: GET /users/{userId}/events/{eventId}/requests
    List<ParticipationRequest> findAllByEventId(Long eventId);

    // 3. Проверить, подавал ли уже пользователь заявку на это событие
    // Защита от дублей при: POST /users/{userId}/requests (Должно бросать ConflictException)
    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // 4. Посчитать количество заявок на событие с определенным статусом
    // Нужно при добавлении новой заявки, чтобы проверить: не превышен ли participantLimit?
    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    // 5. Получить список заявок по их ID и ID события (для массового обновления статусов)
    // Используется в: PATCH /users/{userId}/events/{eventId}/requests
    List<ParticipationRequest> findAllByEventIdAndIdIn(Long eventId, List<Long> ids);
}