package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLRequest {
  QLEntityType entityType;
  List<QLFilter> filters;
  List<QLGroupBy> groupBy;
  QLAggregateFunction aggregationFunction;
  QLTimeSeriesAggregation groupByTime;
}
