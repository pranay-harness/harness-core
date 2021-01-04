package io.harness.steps.common.pipeline;

import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.properties.NGProperties;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("pipelineSetupStepParameters")
public class PipelineSetupStepParameters extends PipelineInfoConfig implements StepParameters {
  String childNodeID;

  @Builder(builderMethodName = "newBuilder")
  public PipelineSetupStepParameters(String uuid, String name, String identifier, ParameterField<String> description,
      Map<String, String> tags, List<NGVariable> variables, NGProperties properties,
      List<StageElementWrapperConfig> stages, String childNodeID) {
    super(uuid, name, identifier, description, tags, variables, properties, stages);
    this.childNodeID = childNodeID;
  }

  public static PipelineSetupStepParameters getStepParameters(PipelineInfoConfig infoConfig, String childNodeID) {
    if (infoConfig == null) {
      return PipelineSetupStepParameters.newBuilder().childNodeID(childNodeID).build();
    }
    return new PipelineSetupStepParameters(infoConfig.getUuid(), infoConfig.getName(), infoConfig.getIdentifier(),
        infoConfig.getDescription(), infoConfig.getTags(), infoConfig.getVariables(), infoConfig.getProperties(),
        infoConfig.getStages(), childNodeID);
  }
}
