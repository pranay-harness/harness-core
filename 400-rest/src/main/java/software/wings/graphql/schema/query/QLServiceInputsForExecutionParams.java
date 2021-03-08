package software.wings.graphql.schema.query;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLServiceInputsForExecutionQueryParametersKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLServiceInputsForExecutionParams {
  String entityId;
  String applicationId;
  List<QLVariableInput> variableInputs;
  QLExecutionType executionType;
}
