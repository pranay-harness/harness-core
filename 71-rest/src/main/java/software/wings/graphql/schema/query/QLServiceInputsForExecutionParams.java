package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLServiceInputsForExecutionQueryParametersKeys")
public class QLServiceInputsForExecutionParams {
  String entityId;
  String applicationId;
  List<QLVariableInput> variableInputs;
  QLExecutionType executionType;
}
