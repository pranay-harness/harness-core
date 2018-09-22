package software.wings.service.impl.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.service.impl.analysis.TimeSeries;

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
  private List<TimeSeries> timeSeriesToCollect;

  @Builder
  public PrometheusSetupTestNodeData(String appId, String settingId, String instanceName,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      List<TimeSeries> timeSeriesToCollect) {
    super(appId, settingId, instanceName, instanceElement, hostExpression, workflowId, fromTime, toTime);
    this.timeSeriesToCollect = timeSeriesToCollect;
  }
}
