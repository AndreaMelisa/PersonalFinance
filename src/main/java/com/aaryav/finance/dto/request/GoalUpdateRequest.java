package com.aaryav.finance.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GoalUpdateRequest {
    @Positive(message = "Target amount must be positive")
    private BigDecimal targetAmount;

    private LocalDate targetDate;
}
