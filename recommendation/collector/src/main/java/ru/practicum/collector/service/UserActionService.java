package ru.practicum.collector.service;

import ru.practicum.stats.proto.collector.UserActionProto;

public interface UserActionService {

    void collectUserAction(UserActionProto action);
}