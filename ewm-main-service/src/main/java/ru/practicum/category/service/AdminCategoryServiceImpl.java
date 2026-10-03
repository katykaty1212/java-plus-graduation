package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.CategoryMapper;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.model.Category;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        try {
            Category category = CategoryMapper.toCategory(newCategoryDto);
            category = categoryRepository.save(category);
            log.info("Category created: {}", category);
            return CategoryMapper.toCategoryDto(category);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Category with name '" + newCategoryDto.getName() + "' already exists");
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Category with id=" + catId + " was not found");
        }

        // Проверяем, есть ли события в этой категории
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Category with id=" + catId + " contains events and cannot be deleted");
        }

        categoryRepository.deleteById(catId);
        log.info("Category deleted: id={}", catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, NewCategoryDto updateDto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));

        try {
            category.setName(updateDto.getName());
            Category updated = categoryRepository.save(category);
            log.info("Category updated: {}", updated);
            return CategoryMapper.toCategoryDto(updated);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Category with name '" + updateDto.getName() + "' already exists");
        }
    }
}
