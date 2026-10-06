package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.user.UserShortDto;
import ru.practicum.user.model.User;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public UserShortDto getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
        return UserMapper.toUserShortDto(user);
    }

    @GetMapping("/{id}/exists")
    public Boolean exists(@PathVariable Long id) {
        return userRepository.existsById(id);
    }
}