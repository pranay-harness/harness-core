package software.wings.service.impl.cloudwatch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;

import java.util.List;
import java.util.Map;

/**
 * Created by Pranjal on 09/04/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudWatchSetupTestNodeData extends SetupTestNodeData {
  private String region;
  private String hostName;
  private Map<String, List<CloudWatchMetric>> loadBalancerMetricsByLBName;
  private List<CloudWatchMetric> ec2Metrics;

  @Builder
  public CloudWatchSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      String region, String hostName, Map<String, List<CloudWatchMetric>> loadBalancerMetricsByLBName,
      List<CloudWatchMetric> ec2Metrics) {
    super(
        appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, fromTime, toTime);
    this.region = region;
    this.hostName = hostName;
    this.loadBalancerMetricsByLBName = loadBalancerMetricsByLBName;
    this.ec2Metrics = ec2Metrics;
  }
}
