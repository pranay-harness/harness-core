package io.harness.pms.merger;

import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNUtils;
import io.harness.pms.yaml.PmsYamlUtils;

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
    yamlMap = PmsYamlUtils.readTree(yaml).getNode().getCurrJsonNode();
    fqnToValueMap = FQNUtils.generateFQNMap(yamlMap);
  }

  public PipelineYamlConfig(Map<FQN, Object> fqnToValueMap, JsonNode originalYaml) throws IOException {
    this.fqnToValueMap = fqnToValueMap;
    yamlMap = FQNUtils.generateYamlMap(fqnToValueMap, originalYaml);
    if (yamlMap.size() != 0) {
      yaml = PmsYamlUtils.write(yamlMap).replace("---\n", "");
    }
  }
}
