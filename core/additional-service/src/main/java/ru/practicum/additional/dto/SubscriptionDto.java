package ru.practicum.additional.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.additional.model.SubscriptionStatus;
import ru.practicum.additional.model.SubscriptionType;
import ru.practicum.user.UserShortDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {

    private Long id;
    private UserShortDto subscriber;
    private UserShortDto publisher;
    private SubscriptionStatus status;
    private SubscriptionType type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}