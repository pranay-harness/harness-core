package software.wings.graphql.schema.query;

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
public class QLExecutionInputsToResumePipelineQueryParams {
  private String applicationId;
  private String pipelineExecutionId;
  private String pipelineStageElementId;
  private List<QLVariableInput> variableInputs;
}
