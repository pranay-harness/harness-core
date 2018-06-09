package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.sm.ExecutionStatusData;

/**
 * Created by rsingh on 5/26/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class MetricDataAnalysisResponse extends ExecutionStatusData {
  private MetricAnalysisExecutionData stateExecutionData;
}
