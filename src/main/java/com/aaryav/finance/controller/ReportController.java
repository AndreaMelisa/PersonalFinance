package com.aaryav.finance.controller;

import com.aaryav.finance.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for financial reports and analytics.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** GET /api/reports/monthly/{year}/{month} - Monthly financial report. */
    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<?> getMonthlyReport(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
    }

    /** GET /api/reports/yearly/{year} - Yearly financial report. */
    @GetMapping("/yearly/{year}")
    public ResponseEntity<?> getYearlyReport(@PathVariable int year) {
        return ResponseEntity.ok(reportService.getYearlyReport(year));
    }
}
