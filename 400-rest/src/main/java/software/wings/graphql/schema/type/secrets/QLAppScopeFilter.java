package software.wings.graphql.schema.type.secrets;

import software.wings.graphql.schema.type.QLGenericFilterType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppScopeFilterKeys")
public class QLAppScopeFilter {
  private QLGenericFilterType filterType;
  private String appId;
}
