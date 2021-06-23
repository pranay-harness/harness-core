package io.harness.ccm.graphql.dto.common;

import io.leangen.graphql.annotations.GraphQLNonNull;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimeSeriesDataPoints {
  @GraphQLNonNull List<DataPoint> values;
  @GraphQLNonNull Long time;
}
