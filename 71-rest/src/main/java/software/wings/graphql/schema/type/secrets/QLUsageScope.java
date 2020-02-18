package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUsageScopeKeys")
public class QLUsageScope {
  Set<QLAppEnvScope> appEnvScopes;
}
