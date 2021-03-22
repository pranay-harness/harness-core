package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByAPIKeyKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLExecutedByAPIKey implements QLCause {
  private QLApiKey apiKey;
  private QLExecuteOptions using;
}
