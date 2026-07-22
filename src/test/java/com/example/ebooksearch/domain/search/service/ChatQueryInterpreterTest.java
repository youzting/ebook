package com.example.ebooksearch.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatQueryInterpreterTest {
  private final ChatQueryInterpreter interpreter = new ChatQueryInterpreter();

  @Test
  void extractsCoreKeywordFromNaturalKoreanQuery() {
    assertThat(interpreter.extractKeyword("대출 가능한 AI 입문서 찾아줘")).isEqualTo("AI 입문서");
  }

  @Test
  void keepsOriginalMessageWhenOnlyNoiseWordsRemain() {
    assertThat(interpreter.extractKeyword("전자책 찾아줘")).isEqualTo("전자책 찾아줘");
  }
}
