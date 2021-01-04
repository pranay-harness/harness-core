package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutedByAPIKeyKeys")
public class QLExecutedByAPIKey implements QLCause {
  private QLApiKey apiKey;
  private QLExecuteOptions using;
}
