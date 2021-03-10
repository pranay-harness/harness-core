package io.harness.gitsync.common.helper;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GitSyncUtils {
  static ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public EntityType getEntityTypeFromYaml(String yaml) {
    try {
      final JsonNode jsonNode = objectMapper.readTree(yaml);
      String rootNode = jsonNode.fields().next().getKey();
      return EntityType.getEntityTypeFromYamlRootName(rootNode);
    } catch (IOException e) {
      throw new InvalidRequestException("Unable to parse yaml", e);
    }
  }
}
