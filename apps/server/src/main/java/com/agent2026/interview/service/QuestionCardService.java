package com.agent2026.interview.service;

import com.agent2026.interview.vo.QuestionCardVO;

import java.util.List;

public interface QuestionCardService {

    List<String> listModules();

    List<QuestionCardVO> listCards(String module, String difficulty);
}
