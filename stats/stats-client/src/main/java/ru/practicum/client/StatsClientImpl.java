package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class StatsClientImpl implements StatsClient {

    private static final String STATS_SERVICE_ID = "stats-server";

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public StatsClientImpl(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = new RestTemplate();

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(3000L);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        this.retryTemplate = new RetryTemplate();
        this.retryTemplate.setBackOffPolicy(backOff);
        this.retryTemplate.setRetryPolicy(retryPolicy);
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);
        if (instances.isEmpty()) {
            throw new StatsServerUnavailable(
                    "Сервис статистики не найден в Eureka: " + STATS_SERVICE_ID,
                    null
            );
        }
        return instances.getFirst();
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    @Override
    public void saveHit(EndpointHitDto dto) {
        try {
            restTemplate.postForLocation(makeUri("/hit"), dto);
        } catch (Exception e) {
            log.error("Ошибка отправки статистики: {}", e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(makeUri("/stats").toString())
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", String.join(",", uris));
            }

            URI uri = builder.build().encode().toUri();

            ViewStatsDto[] response = restTemplate.getForObject(uri, ViewStatsDto[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (Exception e) {
            log.error("Ошибка получения статистики: {}", e.getMessage());
            return List.of();
        }
    }
}