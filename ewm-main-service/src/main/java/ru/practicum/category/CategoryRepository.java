package ru.practicum.category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.category.model.Category;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Поиск категории по названию
    Optional<Category> findByName(String name);

    // Проверка существования категории с таким названием
    boolean existsByName(String name);
}