package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLInstanceCount {
  private int count;
  private QLInstanceCountType instanceCountType;
}
