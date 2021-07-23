package io.harness.pms.merger;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.merger.helpers.YamlMapGenerator;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
public class PipelineYamlConfig {
  private String yaml;
  private JsonNode yamlMap;
  private Map<FQN, Object> fqnToValueMap;

  public PipelineYamlConfig(String yaml) throws IOException {
    this.yaml = yaml;
    yamlMap = YamlUtils.readTree(yaml).getNode().getCurrJsonNode();
    fqnToValueMap = FQNMapGenerator.generateFQNMap(yamlMap);
  }

  public PipelineYamlConfig(Map<FQN, Object> fqnToValueMap, JsonNode originalYaml) throws IOException {
    this.fqnToValueMap = fqnToValueMap;
    yamlMap = YamlMapGenerator.generateYamlMap(fqnToValueMap, originalYaml);
    if (yamlMap.size() != 0) {
      yaml = YamlUtils.write(yamlMap).replace("---\n", "");
    }
  }
}
