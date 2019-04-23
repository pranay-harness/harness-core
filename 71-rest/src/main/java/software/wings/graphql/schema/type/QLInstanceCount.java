package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.datafetcher.instance.InstanceCountDataFetcher.InstanceCountType;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLInstanceCount {
  int count;
  InstanceCountType instanceCountType;
}
