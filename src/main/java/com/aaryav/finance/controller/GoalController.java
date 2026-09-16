package com.aaryav.finance.controller;

import com.aaryav.finance.dto.request.GoalRequest;
import com.aaryav.finance.dto.request.GoalUpdateRequest;
import com.aaryav.finance.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for savings goal management endpoints.
 */
@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    /** POST /api/goals - Create a savings goal. */
    @PostMapping
    public ResponseEntity<?> createGoal(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.createGoal(request));
    }

    /** GET /api/goals - List all goals with progress. */
    @GetMapping
    public ResponseEntity<?> getAllGoals() {
        return ResponseEntity.ok(goalService.getAllGoals());
    }

    /** GET /api/goals/{id} - Get a specific goal with progress. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGoalById(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getGoalById(id));
    }

    /** PUT /api/goals/{id} - Update target amount/date. */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGoal(@PathVariable Long id,
                                         @Valid @RequestBody GoalUpdateRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(id, request));
    }

    /** DELETE /api/goals/{id} - Delete a goal. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.deleteGoal(id));
    }
}
