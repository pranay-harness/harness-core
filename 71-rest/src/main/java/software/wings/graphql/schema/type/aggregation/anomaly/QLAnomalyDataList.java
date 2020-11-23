package software.wings.graphql.schema.type.aggregation.anomaly;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLAnomalyDataList {
  List<QLAnomalyData> data;
}
