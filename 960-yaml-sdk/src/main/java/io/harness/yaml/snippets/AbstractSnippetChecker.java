package io.harness.yaml.snippets;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.packages.HarnessPackages.IO_HARNESS;
import static io.harness.packages.HarnessPackages.SOFTWARE_WINGS;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.YamlSdkConfiguration;
import io.harness.yaml.schema.YamlSchemaRoot;
import io.harness.yaml.snippets.bean.YamlSnippetMetaData;
import io.harness.yaml.snippets.bean.YamlSnippetTags;
import io.harness.yaml.snippets.bean.YamlSnippets;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.slf4j.Logger;

public interface AbstractSnippetChecker {
  default void snippetTests(Logger log) throws IOException {
    Reflections reflections = new Reflections(IO_HARNESS, SOFTWARE_WINGS);
    List<Pair<String, Pair<EntityType, ClassLoader>>> snippetsIndex = getIndexResourceFileContent(log, reflections);
    final Class tagsEnum = getTagsEnum(reflections);
    for (Pair<String, Pair<EntityType, ClassLoader>> snippet : snippetsIndex) {
      testIconTagsAreInTags(snippet.getLeft());
      testSnippetHasCorrectResourceFileSpecified(snippet.getLeft(), snippet.getRight().getRight());
      testSnippetsMatchSchema(log, snippet.getLeft(), snippet.getRight().getLeft(), snippet.getRight().getRight());
      testTagsEnumAndXmlInSync(snippet.getLeft(), tagsEnum);
    }
  }

  default void testIconTagsAreInTags(String resource) throws IOException {
    YamlSnippets yamlSnippets = getYamlSnippets(resource);
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      final List<String> tags = yamlSnippetMetaData.getTags();
      final String iconTag = yamlSnippetMetaData.getIconTag();
      if (!tags.contains(iconTag)) {
        throw new InvalidRequestException("Icon incorrectly specified.");
      }
    }
  }

  default List<Pair<String, Pair<EntityType, ClassLoader>>> getIndexResourceFileContent(
      Logger log, Reflections reflections) {
    final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(YamlSchemaRoot.class, true);
    if (isEmpty(classes)) {
      return Collections.emptyList();
    }
    return classes.stream()
        .map(clazz -> {
          try {
            final String snippetIndexPathForEntityType = YamlSchemaUtils.getSnippetIndexPathForEntityType(
                clazz, YamlSdkConfiguration.snippetBasePath, YamlSdkConfiguration.snippetIndexFile);
            final EntityType value = clazz.getAnnotation(YamlSchemaRoot.class).value();
            String snippetMetaData =
                IOUtils.resourceToString(snippetIndexPathForEntityType, StandardCharsets.UTF_8, clazz.getClassLoader());
            return Pair.of(snippetMetaData, Pair.of(value, clazz.getClassLoader()));

          } catch (IOException e) {
            log.info("No Yaml Snippets found for {}", clazz.getCanonicalName());
            e.printStackTrace();
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  default Class getTagsEnum(Reflections reflections) {
    final Set<Class<? extends io.harness.yaml.snippets.bean.YamlSnippetTags>> tagsEnum =
        reflections.getSubTypesOf(YamlSnippetTags.class);
    if (tagsEnum.size() != 1) {
      throw new InvalidRequestException("Tags enum incorrect");
    }
    return tagsEnum.iterator().next();
  }

  default String getSchemaBasePath() {
    return YamlSdkConfiguration.schemaBasePath;
  }

  default void testSnippetHasCorrectResourceFileSpecified(String snippetIndex, ClassLoader classloader)
      throws IOException {
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

  default YamlSnippets getYamlSnippets(String indexResource) {
    XmlMapper xmlMapper = new XmlMapper();
    YamlSnippets yamlSnippets;
    try {
      yamlSnippets = xmlMapper.readValue(indexResource, YamlSnippets.class);

    } catch (Exception e) {
      throw new InvalidRequestException("Cannot parse snippet metadata");
    }
    return yamlSnippets;
  }

  default void testTagsEnumAndXmlInSync(String indexResource, Class tagsEnum) throws IOException {
    YamlSnippets yamlSnippets = getYamlSnippets(indexResource);
    final Set<String> tags = yamlSnippets.getYamlSnippetMetaDataList()
                                 .stream()
                                 .flatMap(yamlSnippetMetaData -> yamlSnippetMetaData.getTags().stream())
                                 .collect(Collectors.toSet());

    for (String tag : tags) {
      Enum.valueOf(tagsEnum, tag);
    }
  }

  default void testSnippetsMatchSchema(
      Logger log, String indexResource, EntityType entityFromYamlType, ClassLoader classLoader) throws IOException {
    YamlSnippets yamlSnippets = getYamlSnippets(indexResource);
    Set<String> errorSnippets = new HashSet<>();
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      final String snippet =
          IOUtils.resourceToString(yamlSnippetMetaData.getResourcePath(), StandardCharsets.UTF_8, classLoader);
      final String schemaBasePath = getSchemaBasePath();
      final String schemaPathForEntityType =
          YamlSchemaUtils.getSchemaPathForEntityType(entityFromYamlType, schemaBasePath);
      final String schema = IOUtils.resourceToString(schemaPathForEntityType, StandardCharsets.UTF_8, classLoader);
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