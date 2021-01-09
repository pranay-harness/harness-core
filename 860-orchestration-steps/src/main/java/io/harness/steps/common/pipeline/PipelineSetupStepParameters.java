package io.harness.steps.common.pipeline;

import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.properties.NGProperties;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@TypeAlias("pipelineSetupStepParameters")
public class PipelineSetupStepParameters implements StepParameters {
  String childNodeID;

  String name;
  String identifier;
  ParameterField<String> description;
  Map<String, String> tags;
  NGProperties properties;
  List<NGVariable> originalVariables;

  @Builder(builderMethodName = "newBuilder")
  public PipelineSetupStepParameters(String childNodeID, String name, String identifier,
      ParameterField<String> description, Map<String, String> tags, NGProperties properties,
      List<NGVariable> originalVariables) {
    this.childNodeID = childNodeID;
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.tags = tags;
    this.properties = properties;
    this.originalVariables = originalVariables;
  }

  public static PipelineSetupStepParameters getStepParameters(PipelineInfoConfig infoConfig, String childNodeID) {
    if (infoConfig == null) {
      return PipelineSetupStepParameters.newBuilder().childNodeID(childNodeID).build();
    }
    return new PipelineSetupStepParameters(childNodeID, infoConfig.getName(), infoConfig.getIdentifier(),
        infoConfig.getDescription(), infoConfig.getTags(), infoConfig.getProperties(), infoConfig.getVariables());
  }
}
