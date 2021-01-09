package io.harness.yaml.snippets.bean;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Maintain Java Map of Individual Snippets Metadata.
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YamlSnippetMetaData {
  String name;
  String description;
  String version;
  /**
   * Tags for a snippet. Would be specified in XML as <pre>
   *     {@code
   *      <tags>
   *          <tag>tag 1</tag>
   *          <tag>tag 2</tag>
   *      </tags>
   *     }
   * </pre>
   * These tags will be used for specifying the type of snippet.
   */
  List<String> tags;
  /**
   * Resource Path of Yaml Snippet.
   */
  String resourcePath;
  /**
   * icon for the tag which UI will display.
   */
  String iconTag;

  /**
   * The entity Type for which the schema is.
   */
  String schemaEntityType;
}
