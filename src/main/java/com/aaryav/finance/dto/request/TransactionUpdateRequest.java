package com.aaryav.finance.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionUpdateRequest {
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String category;

    private String description;
}
