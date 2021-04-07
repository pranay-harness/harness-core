package io.harness.ng.core.utils;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class NGYamlUtils {
  private static ObjectMapper yamlMapper =
      new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .enable(SerializationFeature.INDENT_OUTPUT);

  public static String getYamlString(YamlDTO yamlObject) {
    if (yamlObject == null) {
      return null;
    }
    String yamlString;
    try {
      yamlString = yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(yamlObject);
    } catch (JsonProcessingException e) {
      throw new InvalidRequestException(
          String.format("Cannot create yaml from YamlObject %s", yamlObject.toString()), e);
    }
    yamlString = yamlString.replaceFirst("---\n", "");
    return yamlString;
  }
}
