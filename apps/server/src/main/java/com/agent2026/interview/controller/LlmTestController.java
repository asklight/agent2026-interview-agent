package com.agent2026.interview.controller;

import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.common.Result;
import com.agent2026.interview.param.LlmTestParam;
import com.agent2026.interview.vo.LlmTestVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm")
public class LlmTestController {

    private final TjuLlmClient tjuLlmClient;

    public LlmTestController(TjuLlmClient tjuLlmClient) {
        this.tjuLlmClient = tjuLlmClient;
    }

    @PostMapping("/test")
    public Result<LlmTestVO> test(@Valid @RequestBody LlmTestParam param) {
        return Result.success(tjuLlmClient.chat(param.getMessage()));
    }
}
