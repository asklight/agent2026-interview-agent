package com.agent2026.interview.controller;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.service.QuestionCardService;
import com.agent2026.interview.vo.QuestionCardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/question-cards")
public class QuestionCardController {

    private final QuestionCardService questionCardService;

    public QuestionCardController(QuestionCardService questionCardService) {
        this.questionCardService = questionCardService;
    }

    @GetMapping("/modules")
    public Result<List<String>> modules() {
        return Result.success(questionCardService.listModules());
    }

    @GetMapping
    public Result<List<QuestionCardVO>> cards(@RequestParam(required = false) String module,
                                              @RequestParam(required = false) String difficulty) {
        return Result.success(questionCardService.listCards(module, difficulty));
    }
}
