package com.example.ebooksearch.domain.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ChatSearchRequest(
    @NotBlank(message = "검색할 내용을 입력해 주세요.")
    @Size(max = 200, message = "검색 문장은 200자 이하여야 합니다.")
    String message,
    List<String> libraryIds,
    Integer limitPerLibrary) {}
