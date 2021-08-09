package io.harness.yaml;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
/**
 * Base configs required to use SDK.
 * Fixing config as convention.
 */
@OwnedBy(DX)
public class YamlSdkInitConstants {
  /**
   * Snippets base path.
   */
  public static final String snippetBasePath = "snippets";
  /**
   * Snippets index file.
   */
  public static final String snippetIndexFile = "index.xml";
  /**
   * Schema base path.
   */
  public static final String schemaBasePath = "schema";
}
