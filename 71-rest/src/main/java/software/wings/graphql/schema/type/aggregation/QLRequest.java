package software.wings.graphql.schema.type.aggregation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLRequest {
  QLEntityType entityType;
  List<QLFilter> filters;
  List<QLGroupBy> groupBy;
  QLAggregateFunction aggregationFunction;
  QLTimeSeriesAggregation groupByTime;
}
