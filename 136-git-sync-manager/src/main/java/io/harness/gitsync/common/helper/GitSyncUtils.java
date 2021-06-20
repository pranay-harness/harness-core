package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.remote.NGObjectMapperHelper.configureNGObjectMapper;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.NoSuchElementException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class GitSyncUtils {
  static ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public EntityType getEntityTypeFromYaml(String yaml) throws InvalidRequestException {
    try {
      configureNGObjectMapper(objectMapper);
      objectMapper.configure(Feature.AUTO_CLOSE_SOURCE, true);
      objectMapper.configure(Feature.ALLOW_YAML_COMMENTS, true);
      final JsonNode jsonNode = objectMapper.readTree(yaml);
      String rootNode = jsonNode.fields().next().getKey();
      return EntityType.getEntityTypeFromYamlRootName(rootNode);
    } catch (IOException | NoSuchElementException e) {
      log.info("Could not process the yaml {}", yaml);
      throw new InvalidRequestException("Unable to parse yaml", e);
    }
  }
}
