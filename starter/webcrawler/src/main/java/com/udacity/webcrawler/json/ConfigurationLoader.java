package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
  public CrawlerConfiguration load() {
    // TODO: Fill in this method.
    try (Reader reader = Files.newBufferedReader(path)) {
      // Step 2: Call the static read() method to parse the JSON data from the reader.
      return read(reader);
    } catch (IOException e) {
      // Step 3: If anything goes wrong with file reading, wrap the exception and re-throw it.
      throw new RuntimeException("Could not load configuration from " + path.toString(), e);
    }
//    return new CrawlerConfiguration.Builder().build();
  }

  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  public static CrawlerConfiguration read(Reader reader) throws IOException{
    // This is here to get rid of the unused variable warning.
    ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

    return objectMapper.readValue(reader, CrawlerConfiguration.class);
  }
}
