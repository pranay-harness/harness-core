package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLEnvFilterType;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvScopeFilterKeys")
public class QLEnvScopeFilter {
  private QLEnvFilterType filterType;
  private String envId;
}
