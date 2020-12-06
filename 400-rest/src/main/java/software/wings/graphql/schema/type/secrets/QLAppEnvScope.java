package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppEnvScopesKeys")
public class QLAppEnvScope {
  private QLAppScopeFilter application;
  private QLEnvScopeFilter environment;
}
