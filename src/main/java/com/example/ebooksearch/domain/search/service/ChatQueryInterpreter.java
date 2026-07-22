package com.example.ebooksearch.domain.search.service;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ChatQueryInterpreter {
  private static final List<Pattern> NOISE_PATTERNS = List.of(
      Pattern.compile("(?i)(전자책|이북|ebook|도서|책)"),
      Pattern.compile("(찾아\\s*줘|검색해\\s*줘|검색|찾아|보여\\s*줘|추천해\\s*줘|추천|있어\\??|있나요\\??)"),
      Pattern.compile("(대출\\s*가능한?|빌릴\\s*수\\s*있는|관련된?|관한)"),
      Pattern.compile("(부산|서울시립대|도서관에서?|전체에서?)"));

  public String extractKeyword(String message) {
    String keyword = message == null ? "" : message.trim();
    keyword = keyword.replaceAll("[\"'“”‘’]", " ");
    for (Pattern pattern : NOISE_PATTERNS) {
      keyword = pattern.matcher(keyword).replaceAll(" ");
    }
    keyword = keyword.replaceAll("\\s+", " ").trim();
    return keyword.isBlank() ? (message == null ? "" : message.trim()) : keyword;
  }
}
