package ru.practicum.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.stats.proto.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.stats.proto.dashboard.UserPredictionsRequestProto;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            recommendationService.getRecommendationsForUser(
                    request.getUserId(), request.getMaxResults()
            ).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка GetRecommendationsForUser: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            recommendationService.getSimilarEvents(
                    request.getEventId(), request.getUserId(), request.getMaxResults()
            ).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка GetSimilarEvents: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<Long> eventIds = request.getEventIdList();
            recommendationService.getInteractionsCount(eventIds)
                    .forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка GetInteractionsCount: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}