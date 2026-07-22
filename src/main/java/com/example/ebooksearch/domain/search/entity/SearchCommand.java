package com.example.ebooksearch.domain.search.entity;

import java.util.Set;

public record SearchCommand(String keyword, Set<LibrarySource> sources) {
  public boolean includes(LibrarySource source) {
    return sources == null || sources.isEmpty() || sources.contains(source);
  }
}
