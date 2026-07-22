package com.example.ebooksearch.domain.search.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import java.util.Optional;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LibrarySource {
  SIMIN("simin", "부산시립시민도서관", "https://contents.siminlib.go.kr:8443"),
  BUSAN_ELIB("busan-elib", "부산광역시 전자도서관", "https://library.busan.go.kr/elib/index.do"),
  BUKGU_EBOOK("bukgu-ebook", "부산 북구 전자도서관", "https://ebook.bsbukgu.go.kr/FxLibrary/"),
  GANGSEO("gangseo", "부산강서전자도서관", "https://ebook.bsgangseo.go.kr/FxLibrary/"),
  UOS("uos", "서울시립대 전자도서관", "https://ebook.uos.ac.kr/FxLibrary/index/");

  private final String id;
  private final String displayName;
  private final String homepage;

  LibrarySource(String id, String displayName, String homepage) {
    this.id = id;
    this.displayName = displayName;
    this.homepage = homepage;
  }

  public String id() { return id; }
  public String displayName() { return displayName; }
  public String homepage() { return homepage; }
  public String getId() { return id; }
  public String getDisplayName() { return displayName; }
  public String getHomepage() { return homepage; }

  public static Optional<LibrarySource> fromId(String id) {
    return Arrays.stream(values()).filter(source -> source.id.equals(id)).findFirst();
  }
}
