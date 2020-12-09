package io.harness.pms.inputset;

import io.harness.pms.inputset.fqn.FQN;
import io.harness.pms.inputset.helpers.FQNUtils;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;
import lombok.Data;

@Data
public class PipelineYamlConfig {
  private String yaml;
  private JsonNode yamlMap;
  private Map<FQN, Object> fqnToValueMap;

  public PipelineYamlConfig(String yaml) throws IOException {
    this.yaml = yaml;
    yamlMap = YamlUtils.readTree(yaml).getNode().getCurrJsonNode();
    fqnToValueMap = FQNUtils.generateFQNMap(yamlMap);
  }

  public PipelineYamlConfig(Map<FQN, Object> fqnToValueMap, JsonNode originalYaml) throws IOException {
    this.fqnToValueMap = fqnToValueMap;
    yamlMap = FQNUtils.generateYamlMap(fqnToValueMap, originalYaml);
    if (yamlMap.size() != 0) {
      yaml = YamlUtils.write(yamlMap).replace("---\n", "");
    }
  }
}
