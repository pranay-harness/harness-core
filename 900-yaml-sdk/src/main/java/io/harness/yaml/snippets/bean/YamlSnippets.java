package io.harness.yaml.snippets.bean;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Used for maintaining list of Yaml Snippet metadata.
 * Sample XML which would be required is <pre>
 *     {@code
 *  <YamlSnippets>
 *     <yamlSnippetMetaDataList>
 *         <YamlSnippetMetaData>
 *             <name>Snippet name</name>
 *             <version>1.0</version>
 *             <description>Snippet description</description>
 *             <tags>
 *                 <tag>tag 1</tag>
 *                 <tag>tag 2</tag>
 *             </tags>
 *             <iconTag>tag1</icontag>
 *             <resourcePath>snippets/snippet.yaml<resourcePath>
 *         </YamlSnippetMetaData>
 *     </yamlSnippetMetaDataList>
 * </YamlSnippets>
 *     }
 * </pre>
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YamlSnippets {
  List<YamlSnippetMetaData> yamlSnippetMetaDataList;
}
