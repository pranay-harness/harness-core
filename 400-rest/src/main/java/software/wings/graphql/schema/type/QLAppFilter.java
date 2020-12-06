package software.wings.graphql.schema.type;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppFilterKeys")
public class QLAppFilter {
  private QLGenericFilterType filterType;
  private Set<String> appIds;
}
