package software.wings.graphql.schema.query;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLServiceQueryParametersKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLServiceQueryParameters {
  private String serviceId;
}
