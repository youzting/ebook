package com.example.ebooksearch.domain.search.repository;

import com.example.ebooksearch.domain.search.entity.Book;
import com.example.ebooksearch.domain.search.entity.CatalogSearchResult;
import com.example.ebooksearch.domain.search.entity.LibrarySource;
import com.example.ebooksearch.domain.search.entity.SearchCommand;
import com.example.ebooksearch.infrastructure.http.RemoteHttpClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Repository
public class SiminCatalogRepository implements LibraryCatalogRepository {
  private static final int PAGE_SIZE = 100;
  private static final LibrarySource SOURCE = LibrarySource.SIMIN;
  private final RemoteHttpClient http;
  private final ObjectMapper objectMapper;

  public SiminCatalogRepository(RemoteHttpClient http, ObjectMapper objectMapper) {
    this.http = http;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<CatalogSearchResult> search(SearchCommand command) {
    if (!command.includes(SOURCE)) return List.of();
    String encoded = URLEncoder.encode(command.keyword(), StandardCharsets.UTF_8);
    String searchUrl = SOURCE.homepage() + "/cont-search";
    try {
      JsonNode root = loadPage(encoded, 1);
      List<Book> books = new ArrayList<>();
      addBooks(root, books);
      int totalCount = root.path("totalCount").asInt(books.size());
      int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
      for (int page = 2; page <= totalPages; page++) {
        addBooks(loadPage(encoded, page), books);
      }
      return List.of(CatalogSearchResult.connected(SOURCE, searchUrl, totalCount, null, books));
    } catch (Exception exception) {
      return List.of(CatalogSearchResult.unavailable(SOURCE, searchUrl, "일시적으로 검색 결과를 불러오지 못했습니다."));
    }
  }

  private JsonNode loadPage(String encodedKeyword, int page) throws Exception {
    String apiUrl = SOURCE.homepage() + "/api/contents/search?searchKeyword=" + encodedKeyword
        + "&searchOption=0&sortOption=1&contentType=EB&innerSearchYN=N&innerKeyword="
        + "&detailYn=N&keywordType1=&keyword1=&keywordType2=&keyword2=&keywordType3="
        + "&keyword3=&keyword4=&currentCount=" + page + "&pageCount=" + PAGE_SIZE;
    return objectMapper.readTree(http.get(apiUrl));
  }

  private void addBooks(JsonNode root, List<Book> books) {
    for (JsonNode node : root.path("ContentDataList")) {
      books.add(new Book(
          node.path("contentsKey").asText(), node.path("title").asText(),
          node.path("author").asText(), node.path("publisher").asText(),
          node.path("contentsInfo").asText(), node.path("coverMSizeUrl").asText(),
          SOURCE.homepage() + "/detail?no=" + node.path("contentsKey").asText(),
          node.path("currentLoanCount").asInt() < node.path("copys").asInt()
              ? "대출 가능" : "대출 중",
          SOURCE));
    }
  }
}
