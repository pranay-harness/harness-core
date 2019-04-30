package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLInstanceCount {
  public enum InstanceCountType { TOTAL, NINETY_FIVE_PERCENTILE }

  int count;
  InstanceCountType instanceCountType;
}
