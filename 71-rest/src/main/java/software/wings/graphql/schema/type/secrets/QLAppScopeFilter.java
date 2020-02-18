package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLGenericFilterType;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppScopeFilterKeys")
public class QLAppScopeFilter {
  private QLGenericFilterType filterType;
  private String appId;
}
