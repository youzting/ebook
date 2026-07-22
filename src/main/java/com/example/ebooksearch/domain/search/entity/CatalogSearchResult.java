package com.example.ebooksearch.domain.search.entity;

import java.util.List;

public record CatalogSearchResult(
    LibrarySource source,
    SourceStatus status,
    String message,
    String searchUrl,
    int totalCount,
    Integer totalCopies,
    List<Book> books) {

  public static CatalogSearchResult connected(
      LibrarySource source, String searchUrl, List<Book> books) {
    return connected(source, searchUrl, books.size(), null, books);
  }

  public static CatalogSearchResult connected(
      LibrarySource source, String searchUrl, int totalCount, Integer totalCopies, List<Book> books) {
    return new CatalogSearchResult(
        source, SourceStatus.CONNECTED, "검색 완료", searchUrl, totalCount, totalCopies, books);
  }

  public static CatalogSearchResult linkOnly(
      LibrarySource source, String searchUrl, String message) {
    return new CatalogSearchResult(
        source, SourceStatus.LINK_ONLY, message, searchUrl, 0, null, List.of());
  }

  public static CatalogSearchResult unavailable(
      LibrarySource source, String searchUrl, String message) {
    return new CatalogSearchResult(
        source, SourceStatus.UNAVAILABLE, message, searchUrl, 0, null, List.of());
  }
}
