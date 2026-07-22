package com.example.ebooksearch.domain.search.entity;

public record Book(
    String id,
    String title,
    String author,
    String publisher,
    String description,
    String coverUrl,
    String detailUrl,
    String availability,
    LibrarySource source) {}
