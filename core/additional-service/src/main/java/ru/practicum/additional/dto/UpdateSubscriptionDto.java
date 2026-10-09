package ru.practicum.additional.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.additional.model.SubscriptionStatus;
import ru.practicum.additional.model.SubscriptionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionDto {

    private SubscriptionType type;
    private SubscriptionStatus status;
}