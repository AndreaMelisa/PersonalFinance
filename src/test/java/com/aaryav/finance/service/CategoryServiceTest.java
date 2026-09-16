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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("test@example.com").build();
        mockSecurityContext("test@example.com");
        lenient().when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Should return all accessible categories")
    void getAllCategories_Success() {
        Category salary = Category.builder().name("Salary").type(CategoryType.INCOME).custom(false).build();
        Category custom = Category.builder().name("Freelance").type(CategoryType.INCOME).custom(true).user(testUser).build();
        when(categoryRepository.findAllAccessibleByUser(1L)).thenReturn(List.of(salary, custom));

        Map<String, List<CategoryResponse>> result = categoryService.getAllCategories();

        assertThat(result.get("categories")).hasSize(2);
        assertThat(result.get("categories").get(0).getName()).isEqualTo("Salary");
        assertThat(result.get("categories").get(0).isCustom()).isFalse();
        assertThat(result.get("categories").get(1).isCustom()).isTrue();
    }

    @Test
    @DisplayName("Should create custom category successfully")
    void createCategory_Success() {
        CategoryRequest request = CategoryRequest.builder().name("Freelance").type("INCOME").build();
        when(categoryRepository.existsByNameForUser("Freelance", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result.getName()).isEqualTo("Freelance");
        assertThat(result.getType()).isEqualTo("INCOME");
        assertThat(result.isCustom()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception for duplicate category name")
    void createCategory_Duplicate() {
        CategoryRequest request = CategoryRequest.builder().name("Salary").type("INCOME").build();
        when(categoryRepository.existsByNameForUser("Salary", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid category type")
    void createCategory_InvalidType() {
        CategoryRequest request = CategoryRequest.builder().name("Test").type("INVALID").build();

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid category type");
    }

    @Test
    @DisplayName("Should delete custom category successfully")
    void deleteCategory_Success() {
        Category custom = Category.builder().id(10L).name("Freelance").type(CategoryType.INCOME).custom(true).user(testUser).build();
        when(categoryRepository.findByNameAccessibleByUser("Freelance", 1L)).thenReturn(Optional.of(custom));
        when(transactionRepository.existsByCategoryIdAndUserId(10L, 1L)).thenReturn(false);

        Map<String, String> result = categoryService.deleteCategory("Freelance");

        assertThat(result.get("message")).isEqualTo("Category deleted successfully");
        verify(categoryRepository).delete(custom);
    }

    @Test
    @DisplayName("Should refuse to delete default category")
    void deleteCategory_DefaultCategory() {
        Category salary = Category.builder().name("Salary").type(CategoryType.INCOME).custom(false).build();
        when(categoryRepository.findByNameAccessibleByUser("Salary", 1L)).thenReturn(Optional.of(salary));

        assertThatThrownBy(() -> categoryService.deleteCategory("Salary"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete default");
    }

    @Test
    @DisplayName("Should refuse to delete category with transactions")
    void deleteCategory_InUse() {
        Category custom = Category.builder().id(10L).name("Freelance").type(CategoryType.INCOME).custom(true).user(testUser).build();
        when(categoryRepository.findByNameAccessibleByUser("Freelance", 1L)).thenReturn(Optional.of(custom));
        when(transactionRepository.existsByCategoryIdAndUserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory("Freelance"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("existing transactions");
    }

    @Test
    @DisplayName("Should throw 404 for non-existent category")
    void deleteCategory_NotFound() {
        when(categoryRepository.findByNameAccessibleByUser("NonExistent", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory("NonExistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void mockSecurityContext(String username) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}
