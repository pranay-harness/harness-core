package io.harness.yaml.snippets;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.snippets.bean.YamlSnippetMetaData;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public interface AbstractSnippetChecker {
  default void testIconTagsAreInTags() throws IOException {
    String resource = getIndexResourceFileContent();
    YamlSnippets yamlSnippets = getYamlSnippets(resource);
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      final List<String> tags = yamlSnippetMetaData.getTags();
      final String iconTag = yamlSnippetMetaData.getIconTag();
      if (!tags.contains(iconTag)) {
        throw new InvalidRequestException("Icon incorrectly specified.");
      }
    }
  }

  String getIndexResourceFileContent() throws IOException;

  Class getTagsEnum();

  long getTotalTagsInEnum();

  String getSchemaBasePath();

  default void testSnippetHasCorrectResourceFileSpecified() throws IOException {
    String indexResource = getIndexResourceFileContent();
    YamlSnippets yamlSnippets = getYamlSnippets(indexResource);
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      final String resourcePath = yamlSnippetMetaData.getResourcePath();
      final InputStream resourceAsStream = getTagsEnum().getClassLoader().getResourceAsStream(resourcePath);
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

  default void testTagsEnumAndXmlInSync() throws IOException {
    String indexResource = getIndexResourceFileContent();
    YamlSnippets yamlSnippets = getYamlSnippets(indexResource);
    final Set<String> tags = yamlSnippets.getYamlSnippetMetaDataList()
                                 .stream()
                                 .flatMap(yamlSnippetMetaData -> yamlSnippetMetaData.getTags().stream())
                                 .collect(Collectors.toSet());
    Class clazz = getTagsEnum();
    for (String tag : tags) {
      Enum.valueOf(clazz, tag);
    }
    if (getTotalTagsInEnum() != tags.size()) {
      throw new InvalidRequestException("All tags aren't in sync.");
    }
  }

  default void testSnippetsMatchSchema(Logger log) throws IOException {
    String indexResource = getIndexResourceFileContent();
    YamlSnippets yamlSnippets = getYamlSnippets(indexResource);
    Set<String> errorSnippets = new HashSet<>();
    for (YamlSnippetMetaData yamlSnippetMetaData : yamlSnippets.getYamlSnippetMetaDataList()) {
      final String snippet = IOUtils.resourceToString(
          yamlSnippetMetaData.getResourcePath(), StandardCharsets.UTF_8, getTagsEnum().getClassLoader());
      final String schemaEntityType = yamlSnippetMetaData.getSchemaEntityType();
      final EntityType entityFromYamlType = EntityType.getEntityFromYamlType(schemaEntityType);
      final String schemaBasePath = getSchemaBasePath();
      final String schemaPathForEntityType =
          YamlSchemaUtils.getSchemaPathForEntityType(entityFromYamlType, schemaBasePath);
      final String schema =
          IOUtils.resourceToString(schemaPathForEntityType, StandardCharsets.UTF_8, getTagsEnum().getClassLoader());
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