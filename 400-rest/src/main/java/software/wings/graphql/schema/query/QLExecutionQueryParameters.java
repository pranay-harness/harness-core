package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(innerTypeName = "QLExecutionQueryParametersKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLExecutionQueryParameters {
  private String executionId;
}
