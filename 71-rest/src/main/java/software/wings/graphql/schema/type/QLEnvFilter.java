package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvFilterKeys")
public class QLEnvFilter {
  private Set<QLEnvFilterType> filterTypes;
  private Set<String> envIds;
}
