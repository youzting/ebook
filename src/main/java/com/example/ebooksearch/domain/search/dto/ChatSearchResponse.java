package com.example.ebooksearch.domain.search.dto;

import com.example.ebooksearch.domain.search.entity.Book;
import java.util.List;

public record ChatSearchResponse(
    String keyword,
    String assistantMessage,
    int totalMatches,
    List<Book> books,
    List<LibraryStatusResponse> libraries) {}
