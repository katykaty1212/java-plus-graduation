package ru.practicum.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.user.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Поиск пользователя по email (полезно при аутентификации и проверке уникальности)
    Optional<User> findByEmail(String email);

    // Проверка существования пользователя с данным email
    boolean existsByEmail(String email);
}
