package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {
    private final RestTemplate restTemplate;

    @Value("${stats-server.url:http://localhost:9090}")
    private String serverUrl;

    // отправить статистику на сервер для сохранения в бд
    public void saveHit(EndpointHitDto hit) { //данные о просмотре когда кто-то жмакнул событие
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EndpointHitDto> request = new HttpEntity<>(hit, headers);

            restTemplate.postForEntity(serverUrl + "/hit", request, Void.class);

            log.info("Статистика сохранена: {}.", hit);
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики: {}", e.getMessage());
        }
    }

    // получить статистику

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        try {
            StringBuilder url = new StringBuilder(
                    serverUrl + "/stats?start=" + start + "&end=" + end + "&unique=" + unique); //тут мы слепливаем все данные в URL для отправки GET

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    url.append("&uris=").append(uri); // клею в конец URL если есть конкретные uri(s)
                }
            }

            ViewStatsDto[] response = restTemplate.getForObject(url.toString(), ViewStatsDto[].class); //отправляем запрос на сервер, получаем массив

            return List.of(response); //переделываем массив в лист
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return List.of();
        }
    }
}