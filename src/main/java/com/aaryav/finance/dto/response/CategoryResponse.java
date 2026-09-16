package com.aaryav.finance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryResponse {
    private String name;
    private String type;

    @JsonProperty("isCustom")
    @Getter(onMethod_ = @JsonProperty("isCustom"))
    private boolean custom;

    @JsonProperty("custom")
    public boolean getCustomFlag() {
        return custom;
    }
}
