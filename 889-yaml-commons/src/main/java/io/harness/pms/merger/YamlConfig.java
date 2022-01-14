package io.harness.pms.merger;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.merger.helpers.YamlMapGenerator;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Data
@Slf4j
public class YamlConfig {
  private String yaml;
  private JsonNode yamlMap;
  private Map<FQN, Object> fqnToValueMap;

  public YamlConfig(String yaml) {
    this.yaml = yaml;
    try {
      yamlMap = YamlUtils.readTree(yaml).getNode().getCurrJsonNode();
    } catch (IOException e) {
      log.error("Could not convert yaml to JsonNode. Yaml:\n" + yaml, e);
      throw new InvalidRequestException("Could not convert yaml to JsonNode: " + e.getMessage());
    }
    fqnToValueMap = FQNMapGenerator.generateFQNMap(yamlMap);
  }

  public YamlConfig(Map<FQN, Object> fqnToValueMap, JsonNode originalYaml) {
    this.fqnToValueMap = fqnToValueMap;
    yamlMap = YamlMapGenerator.generateYamlMap(fqnToValueMap, originalYaml);
    if (yamlMap.size() != 0) {
      yaml = YamlUtils.write(yamlMap).replace("---\n", "");
    }
  }
}
