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
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Repository;

@Repository
public class FxLibraryCatalogRepository implements LibraryCatalogRepository {
  private static final int PAGE_SIZE = 20;
  private static final Pattern PRODUCT_ID = Pattern.compile("goView\\('([^']+)'");
  private static final Pattern RESULT_TOTAL =
      Pattern.compile("총\\s*([0-9,]+)종\\s*\\(([0-9,]+)권\\)");
  private final RemoteHttpClient http;

  public FxLibraryCatalogRepository(RemoteHttpClient http) { this.http = http; }

  @Override
  public List<CatalogSearchResult> search(SearchCommand command) {
    List<CompletableFuture<CatalogSearchResult>> searches = new ArrayList<>();
    if (command.includes(LibrarySource.BUKGU_EBOOK)) {
      searches.add(CompletableFuture.supplyAsync(
          () -> searchOne(LibrarySource.BUKGU_EBOOK, "https://ebook.bsbukgu.go.kr", command)));
    }
    if (command.includes(LibrarySource.UOS)) {
      searches.add(CompletableFuture.supplyAsync(
          () -> searchOne(LibrarySource.UOS, "https://ebook.uos.ac.kr", command)));
    }
    if (command.includes(LibrarySource.GANGSEO)) {
      searches.add(CompletableFuture.supplyAsync(
          () -> searchOne(LibrarySource.GANGSEO, "https://ebook.bsgangseo.go.kr", command)));
    }
    return searches.stream().map(CompletableFuture::join).toList();
  }

  private CatalogSearchResult searchOne(LibrarySource source, String baseUrl, SearchCommand command) {
    String encoded = URLEncoder.encode(command.keyword(), StandardCharsets.UTF_8);
    String searchUrl = buildSearchUrl(baseUrl, encoded, 1);
    try {
      Document document = Jsoup.parse(http.get(searchUrl), baseUrl);
      List<Book> books = new ArrayList<>();
      addBooks(document, books, source, baseUrl);
      Matcher totalMatcher = RESULT_TOTAL.matcher(document.text());
      int totalCount = books.size();
      Integer totalCopies = null;
      if (totalMatcher.find()) {
        totalCount = Integer.parseInt(totalMatcher.group(1).replace(",", ""));
        totalCopies = Integer.parseInt(totalMatcher.group(2).replace(",", ""));
      }
      int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
      for (int page = 2; page <= totalPages; page++) {
        Document nextPage = Jsoup.parse(http.get(buildSearchUrl(baseUrl, encoded, page)), baseUrl);
        addBooks(nextPage, books, source, baseUrl);
      }
      return CatalogSearchResult.connected(source, searchUrl, totalCount, totalCopies, books);
    } catch (Exception exception) {
      return CatalogSearchResult.unavailable(source, searchUrl, "일시적으로 검색 결과를 불러오지 못했습니다.");
    }
  }

  private String buildSearchUrl(String baseUrl, String encodedKeyword, int page) {
    return baseUrl + "/FxLibrary/product/list/?itemdv=1&sort=3&page=" + page
        + "&itemCount=" + PAGE_SIZE + "&pageCount=10&category=book&middlecategory="
        + "&cateopt=new&group_num=recommand&catenavi=main&category_type=&searchoption=1"
        + "&keyoption=&keyoption2=0&keyword=" + encodedKeyword
        + "&listfilter=all_list&selectview=list_on&searchType=search"
        + "&name=&publisher=&author=&terminal=";
  }

  private void addBooks(
      Document document, List<Book> books, LibrarySource source, String baseUrl) {
    document.select("ul#detail_list > li.item").stream()
        .map(item -> toBook(item, source, baseUrl))
        .forEach(books::add);
  }

  private Book toBook(Element item, LibrarySource source, String baseUrl) {
    Element titleLink = item.selectFirst(".subject a");
    String title = titleLink == null ? "제목 없음" : titleLink.text();
    String href = titleLink == null ? "" : titleLink.attr("href");
    Matcher matcher = PRODUCT_ID.matcher(href);
    String id = matcher.find() ? matcher.group(1) : Integer.toHexString(title.hashCode());
    List<Element> info = item.select(".info .i1").first() == null
        ? List.of() : item.select(".info .i1").first().select("li");
    String author = info.size() > 0 ? info.get(0).text().replaceFirst("\\s+저$", "") : "저자 정보 없음";
    String publisher = info.size() > 1 ? info.get(1).text() : "출판사 정보 없음";
    Element cover = item.selectFirst(".thumb img");
    Element description = item.selectFirst(".info .i3");
    return new Book(
        id, title, author, publisher,
        description == null ? "" : description.text(),
        cover == null ? "" : cover.absUrl("src"),
        baseUrl + "/FxLibrary/product/view/?num=" + id,
        "lend_Y".equals(item.attr("name")) ? "대출 가능" : "대출 중",
        source);
  }
}
