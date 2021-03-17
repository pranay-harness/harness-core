package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByAPIKeyKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLExecutedByAPIKey implements QLCause {
  private QLApiKey apiKey;
  private QLExecuteOptions using;
}
