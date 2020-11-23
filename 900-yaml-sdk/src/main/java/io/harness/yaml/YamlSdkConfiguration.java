package io.harness.yaml;

import java.io.InputStream;
import lombok.Builder;
import lombok.Value;

/**
 * Configuration required to use SDK
 */
@Value
@Builder
public class YamlSdkConfiguration {
  /**
   * InputStream for index.xml of snippets.
   */
  InputStream snippetIndex;
  // todo(abhinav): add yaml schema config
}
