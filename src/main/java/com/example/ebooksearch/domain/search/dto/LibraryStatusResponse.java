package com.example.ebooksearch.domain.search.dto;

import com.example.ebooksearch.domain.search.entity.CatalogSearchResult;

public record LibraryStatusResponse(
    String id, String name, String status, String message, String searchUrl,
    int totalCount, Integer totalCopies) {
  public static LibraryStatusResponse from(CatalogSearchResult result) {
    return new LibraryStatusResponse(
        result.source().id(), result.source().displayName(), result.status().name(),
        result.message(), result.searchUrl(), result.totalCount(), result.totalCopies());
  }
}
