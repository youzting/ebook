package com.example.ebooksearch.infrastructure.http;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteHttpClient {
  private static final Logger log = LoggerFactory.getLogger(RemoteHttpClient.class);
  private static final int MAX_ATTEMPTS = 3;
  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  public String get(String url) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(8))
        .header("User-Agent", "Mozilla/5.0 (compatible; MoaBook/0.1; library-search)")
        .header("Accept", "text/html,application/json")
        .GET()
        .build();
    return sendWithRetry(request);
  }

  public String postForm(String url, Map<String, String> form)
      throws IOException, InterruptedException {
    String body = form.entrySet().stream()
        .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
            + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(8))
        .header("User-Agent", "Mozilla/5.0 (compatible; MoaBook/0.1; library-search)")
        .header("Accept", "text/html")
        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    return sendWithRetry(request);
  }

  private String sendWithRetry(HttpRequest request) throws IOException, InterruptedException {
    IOException lastException = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
          throw new IOException("Remote status: " + response.statusCode());
        }
        return response.body();
      } catch (IOException exception) {
        lastException = exception;
        if (attempt == MAX_ATTEMPTS) break;
        log.warn("Remote request failed (attempt {}/{}): {} - {}",
            attempt, MAX_ATTEMPTS, request.uri(), exception.toString());
        Thread.sleep(500L * attempt);
      }
    }
    throw lastException;
  }
}
