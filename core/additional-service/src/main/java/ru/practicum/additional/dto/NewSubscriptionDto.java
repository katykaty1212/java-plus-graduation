package ru.practicum.additional.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.additional.model.SubscriptionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewSubscriptionDto {

    @NotNull(message = "Publisher ID cannot be null")
    private Long publisherId;

    @NotNull(message = "Subscription type cannot be null")
    private SubscriptionType type;
}