package com.agent2026.interview.traininghistory.api;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.identity.security.CurrentUserProvider;
import com.agent2026.interview.traininghistory.application.TrainingHistoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training-history")
public class TrainingHistoryController {
    private final TrainingHistoryService service;
    private final CurrentUserProvider currentUser;

    public TrainingHistoryController(TrainingHistoryService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Result<TrainingHistoryPageResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(service.list(currentUser.requireUserId(), type, status, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public Result<Void> hide(@PathVariable Long id) {
        service.hide(currentUser.requireUserId(), id);
        return Result.success(null);
    }
}
