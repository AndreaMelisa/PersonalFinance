package com.aaryav.finance.controller;

import com.aaryav.finance.dto.request.CategoryRequest;
import com.aaryav.finance.dto.response.CategoryResponse;
import com.aaryav.finance.exception.BadRequestException;
import com.aaryav.finance.exception.DuplicateResourceException;
import com.aaryav.finance.exception.GlobalExceptionHandler;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    private static final String BASE_PATH = "/api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("Debe obtener todas las categorías con 200 OK")
    void shouldGetAllCategories() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        Map<String, List<CategoryResponse>> response = Map.of("categories", List.of(
                CategoryResponse.builder().name("Salary").type("INCOME").custom(false).build(),
                CategoryResponse.builder().name("Food").type("EXPENSE").custom(false).build()));
        when(categoryService.getAllCategories()).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/categories")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].name").value("Salary"))
                .andExpect(jsonPath("$.categories[0].type").value("INCOME"))
                .andExpect(jsonPath("$.categories[1].name").value("Food"));
        verify(categoryService).getAllCategories();
    }

    @Test
    @DisplayName("Debe crear una categoría válida y responder 201 Created")
    void shouldCreateCategorySuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        CategoryRequest request = CategoryRequest.builder()
                .name("Groceries")
                .type("EXPENSE")
                .build();
        CategoryResponse response = CategoryResponse.builder()
                .name("Groceries")
                .type("EXPENSE")
                .custom(true)
                .build();
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/categories")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.isCustom").value(true));

        ArgumentCaptor<CategoryRequest> captor = ArgumentCaptor.forClass(CategoryRequest.class);
        verify(categoryService).createCategory(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Groceries");
        assertThat(captor.getValue().getType()).isEqualTo("EXPENSE");
    }

    @Test
    @DisplayName("Debe rechazar una categoría con nombre o tipo vacío")
    void shouldRejectBlankCategoryFields() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        CategoryRequest request = CategoryRequest.builder().name("").type("").build();

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/categories")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Category name is required")))
                .andExpect(jsonPath("$.message").value(containsString("Category type is required")));
        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("Debe eliminar una categoría correctamente con 200 OK")
    void shouldDeleteCategorySuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        when(categoryService.deleteCategory("Groceries"))
                .thenReturn(Map.of("message", "Category deleted successfully"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(delete(BASE_PATH + "/categories/Groceries")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
        verify(categoryService).deleteCategory("Groceries");
    }

    @Test
    @DisplayName("Debe manejar una categoría duplicada con 409 Conflict")
    void shouldHandleDuplicateCategoryConflict() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        CategoryRequest request = CategoryRequest.builder()
                .name("Groceries")
                .type("EXPENSE")
                .build();
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new DuplicateResourceException("Category with this name already exists"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/categories")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category with this name already exists"));

        ArgumentCaptor<CategoryRequest> captor = ArgumentCaptor.forClass(CategoryRequest.class);
        verify(categoryService).createCategory(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Groceries");
        assertThat(captor.getValue().getType()).isEqualTo("EXPENSE");
    }

    @Test
    @DisplayName("Debe manejar eliminación prohibida de una categoría por defecto")
    void shouldHandleForbiddenCategoryDeletion() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        doThrow(new BadRequestException("Cannot delete default categories"))
                .when(categoryService).deleteCategory("Food");

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(delete(BASE_PATH + "/categories/Food")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete default categories"));
        verify(categoryService).deleteCategory("Food");
    }

    @Test
    @DisplayName("Debe manejar categoría inexistente al intentar eliminarla")
    void shouldHandleNotFoundCategoryDeletion() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        doThrow(new ResourceNotFoundException("Category not found"))
                .when(categoryService).deleteCategory("MissingCategory");

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(delete(BASE_PATH + "/categories/MissingCategory")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
        verify(categoryService).deleteCategory("MissingCategory");
    }
}
