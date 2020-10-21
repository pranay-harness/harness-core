package software.wings.graphql.schema.mutation.pipeline.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLRuntimeExecutionInputs {
  String clientMutationId;

  String applicationId;
  String pipelineExecutionId;
  String pipelineStageElementId;

  List<QLVariableInput> variableInputs;
  List<QLServiceInput> serviceInputs;
}
