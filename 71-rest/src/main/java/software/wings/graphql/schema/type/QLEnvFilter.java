package software.wings.graphql.schema.type;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvFilterKeys")
public class QLEnvFilter {
  private Set<QLEnvFilterType> filterTypes;
  private Set<String> envIds;
}
