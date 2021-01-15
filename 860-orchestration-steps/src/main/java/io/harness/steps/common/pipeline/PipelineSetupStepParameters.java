package io.harness.steps.common.pipeline;

import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
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

  String executionId;
  int sequenceId;

  @Builder(builderMethodName = "newBuilder")
  public PipelineSetupStepParameters(String childNodeID, String name, String identifier,
      ParameterField<String> description, Map<String, String> tags, NGProperties properties,
      List<NGVariable> originalVariables, String executionId, int sequenceId) {
    this.childNodeID = childNodeID;
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.tags = tags;
    this.properties = properties;
    this.originalVariables = originalVariables;
    this.executionId = executionId;
    this.sequenceId = sequenceId;
  }

  public static PipelineSetupStepParameters getStepParameters(
      PlanCreationContext ctx, PipelineInfoConfig infoConfig, String childNodeID) {
    PlanCreationContextValue planCreationContextValue = ctx.getGlobalContext().get("metadata");
    ExecutionMetadata executionMetadata = planCreationContextValue.getMetadata();
    if (infoConfig == null) {
      return PipelineSetupStepParameters.newBuilder()
          .childNodeID(childNodeID)
          .executionId(executionMetadata.getExecutionUuid())
          .sequenceId(executionMetadata.getRunSequence())
          .build();
    }
    return new PipelineSetupStepParameters(childNodeID, infoConfig.getName(), infoConfig.getIdentifier(),
        infoConfig.getDescription(), infoConfig.getTags(), infoConfig.getProperties(), infoConfig.getVariables(),
        executionMetadata.getExecutionUuid(), executionMetadata.getRunSequence());
  }
}
