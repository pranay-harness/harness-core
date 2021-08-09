package software.wings.graphql.schema.mutation.pipeline.input;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLRuntimeExecutionInputs {
  String clientMutationId;

  String applicationId;
  String pipelineExecutionId;
  String pipelineStageElementId;

  List<QLVariableInput> variableInputs;
  List<QLServiceInput> serviceInputs;
}
