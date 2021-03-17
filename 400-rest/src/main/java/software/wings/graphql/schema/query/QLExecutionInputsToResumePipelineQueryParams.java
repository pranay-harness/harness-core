package software.wings.graphql.schema.query;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "QLExecutionInputsToResumePipelineQueryParamsKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLExecutionInputsToResumePipelineQueryParams {
  private String applicationId;
  private String pipelineExecutionId;
  private String pipelineStageElementId;
  private List<QLVariableInput> variableInputs;
}
