package ru.practicum.client;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.util.List;

public interface StatsClient {
    void hit(EndpointHitDto dto);

    List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique);
}