package software.wings.graphql.datafetcher.instance;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.datafetcher.instance.InstanceCountDataFetcher.InstanceCountType;

@Value
@Builder
public class QLInstanceCount {
  int count;
  InstanceCountType instanceCountType;
}
