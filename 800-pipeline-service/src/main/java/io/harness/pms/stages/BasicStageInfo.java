package io.harness.pms.stages;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.YamlNode;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@OwnedBy(PIPELINE)
@Value
@Builder
public class BasicStageInfo {
  String identifier;
  String name;
  String type;
  String yaml;
  YamlNode stageYamlNode;
  @Wither List<String> stagesRequired;
  @Wither boolean isToBeBlocked;
}
