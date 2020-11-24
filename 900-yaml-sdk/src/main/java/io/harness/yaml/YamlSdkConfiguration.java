package io.harness.yaml;

import java.io.InputStream;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration required to use SDK
 */
@Data
@Builder
public class YamlSdkConfiguration {
  /**
   * InputStream for index.xml of snippets.
   */
  InputStream snippetIndex;
  // todo(abhinav): add yaml schema config
}
