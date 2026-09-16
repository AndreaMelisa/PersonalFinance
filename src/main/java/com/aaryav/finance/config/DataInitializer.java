package com.aaryav.finance.config;

import com.aaryav.finance.entity.Category;
import com.aaryav.finance.entity.CategoryType;
import com.aaryav.finance.repository.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds the 7 default categories on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(Category.builder().name("Salary").type(CategoryType.INCOME).custom(false).build());
            categoryRepository.save(Category.builder().name("Food").type(CategoryType.EXPENSE).custom(false).build());
            categoryRepository.save(Category.builder().name("Rent").type(CategoryType.EXPENSE).custom(false).build());
            categoryRepository.save(Category.builder().name("Transportation").type(CategoryType.EXPENSE).custom(false).build());
            categoryRepository.save(Category.builder().name("Entertainment").type(CategoryType.EXPENSE).custom(false).build());
            categoryRepository.save(Category.builder().name("Healthcare").type(CategoryType.EXPENSE).custom(false).build());
            categoryRepository.save(Category.builder().name("Utilities").type(CategoryType.EXPENSE).custom(false).build());
            log.info("Default categories seeded successfully");
        }
    }
}
