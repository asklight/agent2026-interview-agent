package com.agent2026.interview.projectdeepdive.api;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.projectdeepdive.application.ProjectProfileApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project-profiles")
public class ProjectProfileController {

    public static final String RESOURCE_TOKEN_HEADER = "X-Resource-Token";

    private final ProjectProfileApplicationService applicationService;

    public ProjectProfileController(ProjectProfileApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public Result<CreateProjectProfileResponse> create(@Valid @RequestBody CreateProjectProfileRequest request) {
        return Result.success(CreateProjectProfileResponse.from(applicationService.create(request.description())));
    }

    @GetMapping("/{profileId}")
    public Result<ProjectProfileResponse> get(@PathVariable Long profileId,
                                              @RequestHeader(value = RESOURCE_TOKEN_HEADER, required = false)
                                              String resourceToken) {
        return Result.success(ProjectProfileResponse.from(applicationService.get(profileId, resourceToken)));
    }

    @PostMapping("/{profileId}/analyze")
    public Result<ProjectProfileResponse> analyze(@PathVariable Long profileId,
                                                  @RequestHeader(value = RESOURCE_TOKEN_HEADER, required = false)
                                                  String resourceToken) {
        return Result.success(ProjectProfileResponse.from(applicationService.analyze(profileId, resourceToken)));
    }

    @PatchMapping("/{profileId}")
    public Result<ProjectProfileResponse> patch(@PathVariable Long profileId,
                                                @RequestHeader(value = RESOURCE_TOKEN_HEADER, required = false)
                                                String resourceToken,
                                                @Valid @RequestBody PatchProjectProfileRequest request) {
        return Result.success(ProjectProfileResponse.from(
                applicationService.patch(profileId, resourceToken, request.toCommand())));
    }

    @PostMapping("/{profileId}/confirm")
    public Result<ProjectProfileResponse> confirm(@PathVariable Long profileId,
                                                  @RequestHeader(value = RESOURCE_TOKEN_HEADER, required = false)
                                                  String resourceToken) {
        return Result.success(ProjectProfileResponse.from(applicationService.confirm(profileId, resourceToken)));
    }
}
