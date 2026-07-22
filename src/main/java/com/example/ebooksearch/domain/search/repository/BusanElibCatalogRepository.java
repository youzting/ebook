package com.example.ebooksearch.domain.search.repository;

import com.example.ebooksearch.domain.search.entity.Book;
import com.example.ebooksearch.domain.search.entity.CatalogSearchResult;
import com.example.ebooksearch.domain.search.entity.LibrarySource;
import com.example.ebooksearch.domain.search.entity.SearchCommand;
import com.example.ebooksearch.infrastructure.http.RemoteHttpClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Repository;

@Repository
public class BusanElibCatalogRepository implements LibraryCatalogRepository {
  private static final LibrarySource SOURCE = LibrarySource.BUSAN_ELIB;
  private static final int PAGE_SIZE = 50;
  private static final String TABLE_URL =
      "https://library.busan.go.kr/elib/module/elib/search/table.do";
  private static final Pattern TOTAL_COUNT =
      Pattern.compile("book_totalDataCount'\\)\\.text\\('([0-9,]+)'\\)");
  private static final Pattern PUBLISH_DATE =
      Pattern.compile(",\\s*\\d{4}-\\d{2}-\\d{2}$");

  private final RemoteHttpClient http;

  public BusanElibCatalogRepository(RemoteHttpClient http) {
    this.http = http;
  }

  @Override
  public List<CatalogSearchResult> search(SearchCommand command) {
    if (!command.includes(SOURCE)) return List.of();

    String encoded = URLEncoder.encode(command.keyword(), StandardCharsets.UTF_8);
    String searchUrl = "https://library.busan.go.kr/elib/module/elib/search/index.do"
        + "?menu_idx=2&search_text=" + encoded;
    try {
      Document firstPage = loadPage(command.keyword(), 1);
      int totalCount = readTotalCount(firstPage);
      List<Book> books = new ArrayList<>();
      addBooks(firstPage, books, command.keyword());

      int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
      for (int page = 2; page <= totalPages; page++) {
        addBooks(loadPage(command.keyword(), page), books, command.keyword());
      }
      return List.of(
          CatalogSearchResult.connected(SOURCE, searchUrl, totalCount, null, books));
    } catch (Exception exception) {
      return List.of(CatalogSearchResult.unavailable(
          SOURCE, searchUrl, "일시적으로 검색 결과를 불러오지 못했습니다."));
    }
  }

  private Document loadPage(String keyword, int page) throws Exception {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("menu_idx", "2");
    form.put("type", "EBK");
    form.put("search_type", "");
    form.put("author_name", "");
    form.put("book_pubname", "");
    form.put("book_year", "");
    form.put("device", "");
    form.put("viewPage", Integer.toString(page));
    form.put("rowCount", Integer.toString(PAGE_SIZE));
    form.put("search_text", keyword);
    return Jsoup.parse(http.postForm(TABLE_URL, form), TABLE_URL);
  }

  private int readTotalCount(Document document) {
    Matcher matcher = TOTAL_COUNT.matcher(document.html());
    return matcher.find()
        ? Integer.parseInt(matcher.group(1).replace(",", ""))
        : document.select("form#searchTableForm > div.row").size();
  }

  private void addBooks(Document document, List<Book> books, String keyword) {
    for (Element row : document.select("form#searchTableForm > div.row")) {
      Element titleLink = row.selectFirst("a.name.goDetail");
      if (titleLink == null || !"EBK".equals(titleLink.attr("data-type"))) continue;

      String id = titleLink.attr("data-book_idx");
      List<Element> paragraphs = row.select(".bif > p");
      String author = paragraphs.size() > 0 ? paragraphs.get(0).text() : "저자 정보 없음";
      String publisher = paragraphs.size() > 1
          ? PUBLISH_DATE.matcher(paragraphs.get(1).text()).replaceFirst("")
          : "출판사 정보 없음";
      Element cover = row.selectFirst(".thumb img");
      String detailUrl = "https://library.busan.go.kr/elib/module/elib/book/view.do"
          + "?menu_idx=2&book_idx=" + id + "&type=EBK&from_search=Y&search_text="
          + URLEncoder.encode(keyword, StandardCharsets.UTF_8);

      books.add(new Book(
          id,
          titleLink.text(),
          author,
          publisher,
          "",
          cover == null ? "" : cover.absUrl("src"),
          detailUrl,
          "도서관에서 확인",
          SOURCE));
    }
  }
}
