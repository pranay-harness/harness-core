package io.harness.yaml.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.yaml.bean.YamlSnippetMetaData;
import io.harness.yaml.dto.YamlSnippetMetaDataDTO;
import io.harness.yaml.dto.YamlSnippetsDTO;
import io.harness.yaml.helper.YamlSnippetHelper;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class YamlSnippetProvider {
  YamlSnippetHelper yamlSnippetHelper;

  /**
   * @param tags
   * @return metadata {@link YamlSnippetMetaDataDTO} which contains all the tags.
   */
  public YamlSnippetsDTO getYamlSnippetMetaData(List<String> tags) {
    Set<YamlSnippetMetaData> yamlSnippetMetaData = getYamlSnippetMetaDataContainingTag(tags);
    if (yamlSnippetMetaData == null) {
      return null;
    }
    final List<YamlSnippetMetaDataDTO> yamlSnippetMetaDataDTOS =
        yamlSnippetMetaData.stream()
            .map(snippet
                -> YamlSnippetMetaDataDTO.builder()
                       .name(snippet.getName())
                       .description(snippet.getDescription())
                       .version(snippet.getVersion())
                       .identifier(yamlSnippetHelper.getIdentifier(snippet))
                       .tags(snippet.getTags())
                       .iconTag(snippet.getIconTag())
                       .build())
            .collect(Collectors.toList());
    return YamlSnippetsDTO.builder().yamlSnippets(yamlSnippetMetaDataDTOS).build();
  }

  private Set<YamlSnippetMetaData> getYamlSnippetMetaDataContainingTag(List<String> tags) {
    if (isEmpty(tags)) {
      return null;
    }
    final Map<String, Set<YamlSnippetMetaData>> tagMap = yamlSnippetHelper.getTagSnippetMap();

    Set<YamlSnippetMetaData> yamlSnippetMetaData = tagMap.getOrDefault(tags.get(0), null);
    if (isEmpty(yamlSnippetMetaData)) {
      return null;
    }
    for (int i = 1; i < tags.size(); i++) {
      yamlSnippetMetaData =
          Sets.intersection(yamlSnippetMetaData, tagMap.getOrDefault(tags.get(i), Collections.emptySet()));
    }
    return yamlSnippetMetaData;
  }

  /**
   * @param identifier {@link YamlSnippetMetaDataDTO}
   * @return Snippet as plain string reading resource file given in XML config.
   */
  public String getYamlSnippet(String identifier) {
    final Map<String, YamlSnippetMetaData> identifierSnippetMap = yamlSnippetHelper.getIdentifierSnippetMap();
    final YamlSnippetMetaData yamlSnippetMetaData = identifierSnippetMap.getOrDefault(identifier, null);
    if (yamlSnippetMetaData == null) {
      throw new InvalidRequestException("Yaml snippet not found for given identifier");
    }
    try {
      final InputStream inputStream =
          getClass().getClassLoader().getResourceAsStream(yamlSnippetMetaData.getResourcePath());
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    } catch (Exception e) {
      throw new InvalidRequestException("Couldn't find snippet for given identifier.", e);
    }
  }
}
