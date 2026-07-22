package com.example.ebooksearch.domain.search.service;

import com.example.ebooksearch.domain.search.dto.ChatSearchRequest;
import com.example.ebooksearch.domain.search.dto.ChatSearchResponse;
import com.example.ebooksearch.domain.search.dto.LibraryStatusResponse;
import com.example.ebooksearch.domain.search.entity.CatalogSearchResult;
import com.example.ebooksearch.domain.search.entity.LibrarySource;
import com.example.ebooksearch.domain.search.entity.SearchCommand;
import com.example.ebooksearch.domain.search.repository.LibraryCatalogRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;

@Service
public class UnifiedSearchService {
  private final List<LibraryCatalogRepository> repositories;
  private final ChatQueryInterpreter interpreter;

  public UnifiedSearchService(
      List<LibraryCatalogRepository> repositories, ChatQueryInterpreter interpreter) {
    this.repositories = repositories;
    this.interpreter = interpreter;
  }

  public ChatSearchResponse search(ChatSearchRequest request) {
    String keyword = interpreter.extractKeyword(request.message());
    Set<LibrarySource> selected = resolveSources(request.libraryIds());
    SearchCommand command = new SearchCommand(keyword, selected);

    List<CompletableFuture<List<CatalogSearchResult>>> futures = repositories.stream()
        .map(repository -> CompletableFuture.supplyAsync(() -> repository.search(command)))
        .toList();

    List<CatalogSearchResult> catalogs = futures.stream()
        .flatMap(future -> future.join().stream())
        .filter(result -> command.includes(result.source()))
        .sorted(Comparator.comparing(result -> result.source().ordinal()))
        .toList();

    var books = catalogs.stream().flatMap(result -> result.books().stream()).toList();
    var statuses = catalogs.stream().map(LibraryStatusResponse::from).toList();
    long connected = catalogs.stream().filter(result -> result.status().name().equals("CONNECTED")).count();
    int totalMatches = catalogs.stream().mapToInt(CatalogSearchResult::totalCount).sum();

    String answer = totalMatches == 0
        ? "‘%s’ 검색 결과를 바로 가져오지 못했어요. 아래 도서관별 검색 링크에서 같은 검색어로 확인해 보세요."
            .formatted(keyword)
        : "‘%s’ 관련 전자책 총 %d종 중 %d종을 표시합니다. %d개 도서관의 공개 검색 결과를 모았습니다."
            .formatted(keyword, totalMatches, books.size(), connected);

    return new ChatSearchResponse(keyword, answer, totalMatches, books, statuses);
  }

  private Set<LibrarySource> resolveSources(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return new HashSet<>(Arrays.asList(LibrarySource.values()));
    }
    Set<LibrarySource> resolved = new HashSet<>();
    ids.forEach(id -> LibrarySource.fromId(id).ifPresent(resolved::add));
    return resolved.isEmpty() ? new HashSet<>(Arrays.asList(LibrarySource.values())) : resolved;
  }
}
