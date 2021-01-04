package software.wings.graphql.schema.type.secrets;

import software.wings.graphql.schema.type.QLEnvFilterType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvScopeFilterKeys")
public class QLEnvScopeFilter {
  private QLEnvFilterType filterType;
  private String envId;
}
