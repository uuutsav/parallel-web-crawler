package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;
  private final PageParserFactory parserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @TargetParallelism int threadCount,
          @MaxDepth int maxDepth,
          @IgnoredUrls List<Pattern> ignoredUrls,
          PageParserFactory parserFactory) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.parserFactory = parserFactory;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
    ConcurrentHashMap.KeySetView<String, Boolean> visitedUrls = ConcurrentHashMap.newKeySet();

    for (String url : startingUrls) {
      pool.invoke(new CrawlTask(url, deadline, 0, wordCounts, visitedUrls));
    }

    if (wordCounts.isEmpty()) {
      return new CrawlResult.Builder()
              .setUrlsVisited(visitedUrls.size())
              .build();
    }

    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  private class CrawlTask extends RecursiveAction {
    private final String url;
    private final Instant deadline;
    private final int depth;
    private final ConcurrentMap<String, Integer> wordCounts;
    private final ConcurrentHashMap.KeySetView<String, Boolean> visitedUrls;

    CrawlTask(String url, Instant deadline, int depth, ConcurrentMap<String, Integer> wordCounts, ConcurrentHashMap.KeySetView<String, Boolean> visitedUrls) {
      this.url = url;
      this.deadline = deadline;
      this.depth = depth;
      this.wordCounts = wordCounts;
      this.visitedUrls = visitedUrls;
    }

    @Override
    protected void compute() {
      // Base Case 1: Stop if the current depth exceeds maxDepth.
      if (depth >= maxDepth) {
        return;
      }
      // Base Case 2: Stop if the deadline has passed.
      if (clock.instant().isAfter(deadline)) {
        return;
      }
      // Base Case 3: Stop if the URL matches any of the ignored patterns.
      for (Pattern pattern : ignoredUrls) {
        if (pattern.matcher(url).matches()) {
          return;
        }
      }
      // Base Case 4: Stop if the URL has already been visited.
      // The add() method of KeySetView is atomic. It returns false if the element was already present.
      if (!visitedUrls.add(url)) {
        return;
      }

      // Parse the page to get its content (words and links).
      PageParser.Result result = parserFactory.get(url).parse();

      // Atomically update the word counts for every word found on the page.
      // The merge() method of ConcurrentHashMap is thread-safe.
      for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
        wordCounts.merge(e.getKey(), e.getValue(), Integer::sum);
      }

      List<CrawlTask> subtasks = new ArrayList<>();
      // For each link found on the page, create a new subtask.
      for (String link : result.getLinks()) {
        subtasks.add(new CrawlTask(link, deadline, depth + 1, wordCounts, visitedUrls));
      }
      // Invoke all subtasks. The ForkJoinPool will execute them in parallel.
      invokeAll(subtasks);
    }
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}