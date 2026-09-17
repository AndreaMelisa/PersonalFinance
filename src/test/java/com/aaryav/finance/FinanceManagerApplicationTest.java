package com.aaryav.finance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FinanceManagerApplicationTest {

    @Test
    void main_ShouldStartApplication() {
        String[] args = {};
        assertDoesNotThrow(() -> FinanceManagerApplication.main(args));
    }
}