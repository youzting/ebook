package com.example.ebooksearch.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ebooksearch.domain.search.dto.ChatSearchRequest;
import com.example.ebooksearch.domain.search.entity.Book;
import com.example.ebooksearch.domain.search.entity.CatalogSearchResult;
import com.example.ebooksearch.domain.search.entity.LibrarySource;
import com.example.ebooksearch.domain.search.repository.LibraryCatalogRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnifiedSearchServiceTest {
  @Test
  void combinesRepositoryResultsIntoChatResponse() {
    LibraryCatalogRepository repository = command -> List.of(CatalogSearchResult.connected(
        LibrarySource.SIMIN, "https://example.com", List.of(new Book(
            "1", "AI 입문", "홍길동", "모아출판", "", "", "https://example.com/1",
            "대출 가능", LibrarySource.SIMIN))));
    UnifiedSearchService service = new UnifiedSearchService(List.of(repository), new ChatQueryInterpreter());

    var response = service.search(new ChatSearchRequest("AI 책 찾아줘", List.of("simin"), null));

    assertThat(response.keyword()).isEqualTo("AI");
    assertThat(response.books()).hasSize(1);
    assertThat(response.totalMatches()).isEqualTo(1);
    assertThat(response.assistantMessage()).contains("1종");
  }
}
