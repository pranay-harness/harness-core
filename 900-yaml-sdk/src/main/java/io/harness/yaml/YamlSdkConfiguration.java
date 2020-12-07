package io.harness.yaml;

import com.google.inject.Singleton;
import java.io.InputStream;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration required to use SDK
 */
@Data
@Builder
@Singleton
public class YamlSdkConfiguration {
  /**
   * InputStream for index.xml of snippets.
   */
  InputStream snippetIndex;
  /**
   * Schema base path.
   */
  String schemaBasePath;
}
