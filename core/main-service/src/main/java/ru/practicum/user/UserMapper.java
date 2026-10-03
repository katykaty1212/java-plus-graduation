package ru.practicum.user;

import lombok.experimental.UtilityClass;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.model.User;

@UtilityClass
public class UserMapper {

    public static User toUser(NewUserRequest request) {
        if (request == null) {
            return null;
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        return user;
    }

    public static UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());

        return userDto;
    }

    public static UserShortDto toUserShortDto(User user) {
        if (user == null) {
            return null;
        }

        UserShortDto dto = new UserShortDto();
        dto.setId(user.getId());
        dto.setName(user.getName());

        return dto;
    }
}
