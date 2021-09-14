/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.snippets.bean;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

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
 *             <schemaEntityType>Connector</schemaEntityType>
 *         </YamlSnippetMetaData>
 *     </yamlSnippetMetaDataList>
 * </YamlSnippets>
 *     }
 * </pre>
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class YamlSnippets {
  List<YamlSnippetMetaData> yamlSnippetMetaDataList;
}
