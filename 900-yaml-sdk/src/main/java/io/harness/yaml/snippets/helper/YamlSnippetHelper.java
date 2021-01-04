package io.harness.yaml.snippets.helper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.yaml.snippets.bean.YamlSnippetMetaData;
import io.harness.yaml.snippets.bean.YamlSnippets;
import io.harness.yaml.snippets.dto.YamlSnippetMetaDataDTO;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.inject.Singleton;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class YamlSnippetHelper {
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
   * @param snippetMetadata       is the String representation of index.xml {@link YamlSnippetMetaData}
   *
   */
  public void preComputeTagsAndNameMap(String snippetMetadata) {
    YamlSnippets yamlSnippets = getYamlSnippets(snippetMetadata);
    if (yamlSnippets == null || isEmpty(yamlSnippets.getYamlSnippetMetaDataList())) {
      log.info("No Yaml Snippet found while initialising.");
      return;
    }
    preComputeTagMap(yamlSnippets);
    preComputeNameSnippetMap(yamlSnippets);
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
}
