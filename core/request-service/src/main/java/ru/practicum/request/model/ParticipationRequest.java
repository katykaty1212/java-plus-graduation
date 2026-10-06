package ru.practicum.request.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.request.RequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RequestStatus status;

    @Column(name = "created")
    private LocalDateTime createdDate;
}