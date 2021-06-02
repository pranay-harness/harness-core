package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLPodCountTimeSeriesDataPoint {
  private List<QLPodCountDataPoint> values;
  private Long time;
}
