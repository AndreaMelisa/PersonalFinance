package com.aaryav.finance.service;

import com.aaryav.finance.dto.request.CategoryRequest;
import com.aaryav.finance.dto.response.CategoryResponse;
import com.aaryav.finance.entity.Category;
import com.aaryav.finance.entity.CategoryType;
import com.aaryav.finance.entity.User;
import com.aaryav.finance.exception.BadRequestException;
import com.aaryav.finance.exception.DuplicateResourceException;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.repository.CategoryRepository;
import com.aaryav.finance.repository.TransactionRepository;
import com.aaryav.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service managing default and custom categories.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Returns all categories accessible by the current user.
     */
    public Map<String, List<CategoryResponse>> getAllCategories() {
        User user = getCurrentUser();
        List<CategoryResponse> categories = categoryRepository
                .findAllAccessibleByUser(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return Map.of("categories", categories);
    }

    /**
     * Creates a custom category for the current user.
     */
    public CategoryResponse createCategory(CategoryRequest request) {
        User user = getCurrentUser();

        CategoryType type;
        try {
            type = CategoryType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category type. Must be INCOME or EXPENSE");
        }

        if (categoryRepository.existsByNameForUser(request.getName(), user.getId())) {
            throw new DuplicateResourceException("Category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .type(type)
                .custom(true)
                .user(user)
                .build();

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    /**
     * Deletes a custom category by name.
     */
    public Map<String, String> deleteCategory(String name) {
        User user = getCurrentUser();

        // Check default categories first
        Category category = categoryRepository.findByNameAccessibleByUser(name, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.isCustom()) {
            throw new BadRequestException("Cannot delete default categories");
        }

        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Category not found");
        }

        if (transactionRepository.existsByCategoryIdAndUserId(category.getId(), user.getId())) {
            throw new BadRequestException("Cannot delete category with existing transactions");
        }

        categoryRepository.delete(category);
        return Map.of("message", "Category deleted successfully");
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .name(category.getName())
                .type(category.getType().name())
                .custom(category.isCustom())
                .build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
