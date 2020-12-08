package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.yaml.snippets.bean.YamlSnippetMetaData;
import io.harness.yaml.snippets.bean.YamlSnippets;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;

public interface AbstractSnippetTestBase {
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
}
