package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppFilterKeys")
public class QLAppFilter {
  private QLGenericFilterType filterType;
  private Set<String> appIds;
}
