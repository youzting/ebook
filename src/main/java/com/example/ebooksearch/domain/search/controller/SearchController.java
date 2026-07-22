package com.example.ebooksearch.domain.search.controller;

import com.example.ebooksearch.domain.search.dto.ChatSearchRequest;
import com.example.ebooksearch.domain.search.dto.ChatSearchResponse;
import com.example.ebooksearch.domain.search.service.UnifiedSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
  private final UnifiedSearchService unifiedSearchService;

  public SearchController(UnifiedSearchService unifiedSearchService) {
    this.unifiedSearchService = unifiedSearchService;
  }

  @PostMapping("/chat")
  public ResponseEntity<ChatSearchResponse> search(@Valid @RequestBody ChatSearchRequest request) {
    return ResponseEntity.ok(unifiedSearchService.search(request));
  }
}
