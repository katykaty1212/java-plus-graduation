package ru.practicum.category;

import lombok.experimental.UtilityClass;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.model.Category;

@UtilityClass
public class CategoryMapper {

    public static Category toCategory(NewCategoryDto request) {
        if (request == null) {
            return null;
        }

        Category category = new Category();
        category.setName(request.getName());

        return category;
    }

    public static CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }

        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());

        return dto;
    }
}