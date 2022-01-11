/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.snippets.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.YamlSdkInitConstants;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.snippets.bean.YamlSnippetMetaData;
import io.harness.yaml.snippets.bean.YamlSnippets;
import io.harness.yaml.snippets.dto.YamlSnippetMetaDataDTO;
import io.harness.yaml.snippets.dto.YamlSnippetsDTO;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Singleton
@Slf4j
@OwnedBy(DX)
public class YamlSnippetHelper {
  @Inject List<YamlSchemaRootClass> yamlSchemaRootClasses;
  /**
   * Used to maintain Map of tag with snippet metadata. It will be used for fast retrieval of snippet based on tags.
   */
  private static Map<String, Set<YamlSnippetMetaData>> tagSnippetMetaDataMap = new HashMap<>();
  /**
   * Used to maintain Map of identifier{@link YamlSnippetMetaDataDTO}  with snippet metadata.
   * It will be used for fast retrieval of snippet based on tags.
   */
  private static Map<String, YamlSnippetMetaData> identifierSnippetMap = new HashMap<>();

  /**
   * @param snippetMetadata is the String representation of index.xml {@link YamlSnippetMetaData}
   * @param yamlSchemaRootClass
   */
  public void preComputeTagsAndNameMap(String snippetMetadata, YamlSchemaRootClass yamlSchemaRootClass) {
    YamlSnippets yamlSnippets = getYamlSnippets(snippetMetadata);
    if (yamlSnippets == null || isEmpty(yamlSnippets.getYamlSnippetMetaDataList())) {
      log.info("No Yaml Snippet found while initialising.");
      return;
    }
    populateSchemaMetaDataInSnippet(yamlSnippets, yamlSchemaRootClass);
    preComputeTagMap(yamlSnippets);
    preComputeNameSnippetMap(yamlSnippets);
  }

  private void populateSchemaMetaDataInSnippet(YamlSnippets yamlSnippets, YamlSchemaRootClass clazz) {
    yamlSnippets.getYamlSnippetMetaDataList().forEach(yamlSnippetMetaData -> {
      yamlSnippetMetaData.setAvailableAtAccountLevel(clazz.isAvailableAtAccountLevel());
      yamlSnippetMetaData.setAvailableAtOrgLevel(clazz.isAvailableAtOrgLevel());
      yamlSnippetMetaData.setAvailableAtProjectLevel(clazz.isAvailableAtProjectLevel());
      yamlSnippetMetaData.setSchemaEntityType(clazz.getEntityType().getYamlName());
    });
  }

  private void preComputeNameSnippetMap(YamlSnippets yamlSnippets) {
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      identifierSnippetMap.put(getIdentifier(yamlSnippetMetaData), yamlSnippetMetaData);
    }
  }

  private void preComputeTagMap(YamlSnippets yamlSnippets) {
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      for (String tag : yamlSnippetMetaData.getTags()) {
        if (!tagSnippetMetaDataMap.containsKey(tag)) {
          tagSnippetMetaDataMap.put(tag, new HashSet<>());
        }
        tagSnippetMetaDataMap.get(tag).add(yamlSnippetMetaData);
      }
    }
  }

  /**
   * @param snippetMetadata is the String representation of index.xml
   * @return Conversion of snippetMetadata to Java Pojo
   */
  private YamlSnippets getYamlSnippets(String snippetMetadata) {
    YamlSnippets yamlSnippets;
    XmlMapper xmlMapper = new XmlMapper();
    try {
      yamlSnippets = xmlMapper.readValue(snippetMetadata, YamlSnippets.class);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot parse snippet metadata");
    }
    return yamlSnippets;
  }

  public Map<String, Set<YamlSnippetMetaData>> getTagSnippetMap() {
    return tagSnippetMetaDataMap;
  }

  public Map<String, YamlSnippetMetaData> getIdentifierSnippetMap() {
    return identifierSnippetMap;
  }

  /**
   * @param yamlSnippetMetaData
   * @return the slug of YamlMetaData.name + YamlMetaData.version.
   */
  public String getIdentifier(YamlSnippetMetaData yamlSnippetMetaData) {
    String s = yamlSnippetMetaData.getName() + "-" + yamlSnippetMetaData.getVersion();
    final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    final Pattern WHITESPACE = Pattern.compile("[\\s]");
    final Pattern DOTS = Pattern.compile("[\\.]");
    String nowhitespace = WHITESPACE.matcher(s).replaceAll("-");
    String noDots = DOTS.matcher(nowhitespace).replaceAll("-");
    String normalized = Normalizer.normalize(noDots, Form.NFD);
    String slug = NONLATIN.matcher(normalized).replaceAll("");
    return slug.toLowerCase(Locale.ENGLISH);
  }

  /**
   * Initialises snippet which caches and maintains maps for fast retrieval of snippets and snippets metadata {@link
   * YamlSnippetsDTO}.
   *
   * @throws InvalidRequestException when snippets couldn't be initialized.
   */
  public void initializeSnippets() {
    try {
      yamlSchemaRootClasses.forEach(yamlSchemaRootClass -> {
        try {
          final String snippetIndexPathForEntityType =
              YamlSchemaUtils.getSnippetIndexPathForEntityType(yamlSchemaRootClass.getEntityType(),
                  YamlSdkInitConstants.snippetBasePath, YamlSdkInitConstants.snippetIndexFile);
          String snippetMetaData = IOUtils.resourceToString(
              snippetIndexPathForEntityType, StandardCharsets.UTF_8, yamlSchemaRootClass.getClazz().getClassLoader());
          preComputeTagsAndNameMap(snippetMetaData, yamlSchemaRootClass);
          log.info("Initialized Yaml Snippets for {}", yamlSchemaRootClass.getEntityType());
        } catch (IOException e) {
          log.info("No Yaml Snippets found for {}", yamlSchemaRootClass.getEntityType());
        }
      });

    } catch (Exception e) {
      throw new InvalidRequestException("Cannot initialize snippets", e);
    }
  }
}
