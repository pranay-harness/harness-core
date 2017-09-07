package software.wings.api;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.StateExecutionData;

/**
 * Created by rsingh on 5/26/17.
 */
@Data
@Builder
public class MetricDataAnalysisResponse extends ExecutionStatusData {
  private StateExecutionData stateExecutionData;
}
