/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.snippets;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.packages.HarnessPackages.IO_HARNESS;
import static io.harness.packages.HarnessPackages.SOFTWARE_WINGS;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.YamlSdkInitConstants;
import io.harness.yaml.schema.JacksonClassHelper;
import io.harness.yaml.schema.SwaggerGenerator;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.snippets.bean.YamlSnippetMetaData;
import io.harness.yaml.snippets.bean.YamlSnippetTags;
import io.harness.yaml.snippets.bean.YamlSnippets;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;

@Singleton
@Slf4j
@OwnedBy(DX)
public class AbstractSnippetChecker {
  List<YamlSchemaRootClass> yamlSchemaRootClasses;
  ObjectMapper objectMapper;

  @Inject
  public AbstractSnippetChecker(List<YamlSchemaRootClass> yamlSchemaRootClasses, ObjectMapper objectMapper) {
    this.yamlSchemaRootClasses = yamlSchemaRootClasses;
    this.objectMapper = objectMapper;
  }
  public void snippetTests() throws IOException {
    if (isEmpty(yamlSchemaRootClasses)) {
      return;
    }
    Reflections reflections = new Reflections(IO_HARNESS, SOFTWARE_WINGS);
    List<Pair<String, Pair<EntityType, ClassLoader>>> snippetsIndex =
        getIndexResourceFileContent(yamlSchemaRootClasses);
    if (isEmpty(snippetsIndex)) {
      return;
    }
    YamlSchemaGenerator yamlSchemaGenerator = new YamlSchemaGenerator(
        new JacksonClassHelper(objectMapper), new SwaggerGenerator(objectMapper), yamlSchemaRootClasses);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    final Class tagsEnum = getTagsEnum(reflections);
    for (Pair<String, Pair<EntityType, ClassLoader>> snippet : snippetsIndex) {
      testIconTagsAreInTags(snippet.getLeft());
      testSnippetHasCorrectResourceFileSpecified(snippet.getLeft(), snippet.getRight().getRight());
      testSnippetsMatchSchema(
          snippet.getLeft(), snippet.getRight().getLeft(), snippet.getRight().getRight(), entityTypeJsonNodeMap);
      testTagsEnumAndXmlInSync(snippet.getLeft(), tagsEnum);
    }
  }

  void testIconTagsAreInTags(String resource) {
    YamlSnippets yamlSnippets = getYamlSnippets(resource);
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      final List<String> tags = yamlSnippetMetaData.getTags();
      final String iconTag = yamlSnippetMetaData.getIconTag();
      if (!tags.contains(iconTag)) {
        throw new InvalidRequestException("Icon incorrectly specified.");
      }
    }
  }

  List<Pair<String, Pair<EntityType, ClassLoader>>> getIndexResourceFileContent(
      List<YamlSchemaRootClass> yamlSchemaRootClasses) {
    if (isEmpty(yamlSchemaRootClasses)) {
      return Collections.emptyList();
    }
    return yamlSchemaRootClasses.stream()
        .map(clazz -> {
          try {
            final String snippetIndexPathForEntityType = YamlSchemaUtils.getSnippetIndexPathForEntityType(
                clazz.getEntityType(), YamlSdkInitConstants.snippetBasePath, YamlSdkInitConstants.snippetIndexFile);
            final EntityType value = clazz.getEntityType();
            String snippetMetaData = IOUtils.resourceToString(
                snippetIndexPathForEntityType, StandardCharsets.UTF_8, clazz.getClazz().getClassLoader());
            return Pair.of(snippetMetaData, Pair.of(value, clazz.getClazz().getClassLoader()));

          } catch (IOException e) {
            log.info("No Yaml Snippets found for {}", clazz.getEntityType());
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  Class getTagsEnum(Reflections reflections) {
    final Set<Class<? extends YamlSnippetTags>> tagsEnum = reflections.getSubTypesOf(YamlSnippetTags.class);
    if (isEmpty(tagsEnum)) {
      log.info("Enum not registered in this class, should be registered in application module.");
      return null;
    }
    if (tagsEnum.size() != 1) {
      throw new InvalidRequestException("Tags enum incorrect");
    }
    return tagsEnum.iterator().next();
  }

  void testSnippetHasCorrectResourceFileSpecified(String snippetIndex, ClassLoader classloader) throws IOException {
    YamlSnippets yamlSnippets = getYamlSnippets(snippetIndex);
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      final String resourcePath = yamlSnippetMetaData.getResourcePath();
      final InputStream resourceAsStream = classloader.getResourceAsStream(resourcePath);
      String snippetMetaData = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
      if (isEmpty(snippetMetaData)) {
        throw new InvalidRequestException("Snippet resource path incorrect.");
      }
    }
  }

  YamlSnippets getYamlSnippets(String indexResource) {
    XmlMapper xmlMapper = new XmlMapper();
    YamlSnippets yamlSnippets;
    try {
      yamlSnippets = xmlMapper.readValue(indexResource, YamlSnippets.class);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot parse snippet metadata");
    }
    return yamlSnippets;
  }

  void testTagsEnumAndXmlInSync(String indexResource, Class tagsEnum) {
    if (tagsEnum == null) {
      return;
    }
    YamlSnippets yamlSnippets = getYamlSnippets(indexResource);
    final Set<String> tags = yamlSnippets.getYamlSnippetMetaDataList()
                                 .stream()
                                 .flatMap(yamlSnippetMetaData -> yamlSnippetMetaData.getTags().stream())
                                 .collect(Collectors.toSet());

    for (String tag : tags) {
      Enum.valueOf(tagsEnum, tag);
    }
  }

  void testSnippetsMatchSchema(String indexResource, EntityType entityFromYamlType, ClassLoader classLoader,
      Map<EntityType, JsonNode> entityTypeJsonNodeMap) throws IOException {
    YamlSnippets yamlSnippets = getYamlSnippets(indexResource);
    Set<String> errorSnippets = new HashSet<>();
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      final String snippet =
          IOUtils.resourceToString(yamlSnippetMetaData.getResourcePath(), StandardCharsets.UTF_8, classLoader);
      final JsonNode schema = entityTypeJsonNodeMap.get(entityFromYamlType);
      log.info("Validating snippet {} against schema for {} ", yamlSnippetMetaData.getName(), entityFromYamlType);
      JsonSchemaFactory factory =
          JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).build();
      final JsonSchema jsonSchema = factory.getSchema(schema);
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      final JsonNode snippetJsonNode = mapper.readTree(snippet);
      final Set<ValidationMessage> errors = jsonSchema.validate(snippetJsonNode);
      if (isNotEmpty(errors)) {
        errorSnippets.add(yamlSnippetMetaData.getResourcePath());
        log.error("Invalid snippet {}-{} with error {}", yamlSnippetMetaData.getName(),
            yamlSnippetMetaData.getVersion(), errors.toString());
      }
    }
    if (isNotEmpty(errorSnippets)) {
      throw new InvalidRequestException(String.format("Found invalid snippets %s", errorSnippets.toString()));
    }
  }
}
