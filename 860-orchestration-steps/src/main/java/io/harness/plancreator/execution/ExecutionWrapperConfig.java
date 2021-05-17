package io.harness.plancreator.execution;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.yaml.YamlNode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("executionWrapperConfig")
// TODO (sahil) this needs to go to yaml commons right now as we have no module marking it for PMS commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public class ExecutionWrapperConfig {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  @ApiModelProperty(dataType = "io.harness.plancreator.steps.StepElementConfig") JsonNode step;
  @ApiModelProperty(dataType = "io.harness.plancreator.steps.ParallelStepElementConfig") JsonNode parallel;
  @ApiModelProperty(dataType = "io.harness.plancreator.steps.StepGroupElementConfig") JsonNode stepGroup;
}
