package software.wings.graphql.schema.type.secrets;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUsageScopeKeys")
public class QLUsageScope {
  Set<QLAppEnvScope> appEnvScopes;
}
