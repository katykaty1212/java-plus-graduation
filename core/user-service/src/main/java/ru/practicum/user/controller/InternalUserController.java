package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.user.UserShortDto;
import ru.practicum.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/by-ids")
    public List<UserShortDto> getUsers(@RequestParam List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .map(UserMapper::toUserShortDto)
                .collect(Collectors.toList());
    }
}