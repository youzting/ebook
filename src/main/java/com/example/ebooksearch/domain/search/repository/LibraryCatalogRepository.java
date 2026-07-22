package com.example.ebooksearch.domain.search.repository;

import com.example.ebooksearch.domain.search.entity.CatalogSearchResult;
import com.example.ebooksearch.domain.search.entity.SearchCommand;
import java.util.List;

public interface LibraryCatalogRepository {
  List<CatalogSearchResult> search(SearchCommand command);
}
