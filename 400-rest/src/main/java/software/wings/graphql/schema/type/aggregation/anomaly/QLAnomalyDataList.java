package software.wings.graphql.schema.type.aggregation.anomaly;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLAnomalyDataList implements QLData {
  List<QLAnomalyData> data;
}
