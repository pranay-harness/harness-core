package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
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
