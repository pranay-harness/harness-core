package software.wings.service.impl.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Prometheus request payload for TestNodeData.
 * Created by Pranjal on 09/02/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PrometheusSetupTestNodeData extends SetupTestNodeData {
  private List<TimeSeries> timeSeriesToAnalyze;

  @Builder
  public PrometheusSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      List<TimeSeries> timeSeriesToAnalyze, String guid) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.PROMETHEUS, fromTime, toTime);
    this.timeSeriesToAnalyze = timeSeriesToAnalyze;
  }
}
