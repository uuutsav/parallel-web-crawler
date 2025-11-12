package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {
  private final CrawlResult result;

  /**
   * Creates a new {@link CrawlResultWriter} that will write the given {@link CrawlResult}.
   */
  public CrawlResultWriter(CrawlResult result) {
    this.result = Objects.requireNonNull(result);
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Path}.
   *
   * <p>If a file already exists at the path, the existing file should be overwritten.
   *
   * @param path the file path where the crawl result data should be written.
   */
  public void write(Path path) {
    Objects.requireNonNull(path);
    //   1: Use try-with-resources to open a writer to the specified path.
    // This ensures the writer is automatically closed when we're done.
    try (Writer writer = Files.newBufferedWriter(path)) {
      //   2: Call the other write method to perform the actual JSON writing.
      write(writer);
    } catch (IOException e) {
      //   3: If file writing fails, wrap and re-throw the exception.
      throw new RuntimeException("Could not write crawl result to " + path.toString(), e);
    }
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Writer}.
   *
   * @param writer the destination where the crawl result data should be written.
   */
  public void write(Writer writer) {
    Objects.requireNonNull(writer);
    //   1: Create a new ObjectMapper, the main tool for JSON conversion.
    ObjectMapper objectMapper = new ObjectMapper();

    //   2 (The Hint): Disable a feature to prevent Jackson from closing the writer.
    objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    try {
      //   3: Use the writeValue method to convert the 'result' object to a JSON string
      // and send it to the 'writer'. Jackson automatically inspects the CrawlResult
      // object and its public getters to create the JSON structure.
      objectMapper.writeValue(writer, result);
    } catch (IOException e) {
      // 4: If JSON conversion fails, wrap and re-throw the exception.
      throw new RuntimeException("Could not write crawl result as JSON", e);
    }
  }
}