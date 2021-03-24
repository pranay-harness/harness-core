package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvironmentQueryParametersKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEnvironmentQueryParameters {
  private String environmentId;
}
