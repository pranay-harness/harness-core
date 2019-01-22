package software.wings.service.impl.stackdriver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Pranjal on 11/27/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StackDriverSetupTestNodeData extends SetupTestNodeData {
  private Map<String, List<StackDriverMetric>> loadBalancerMetrics = new HashMap<>();

  private Set<StackDriverMetric> vmInstanceMetrics = new HashSet<>();

  @Builder
  public StackDriverSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      Map<String, List<StackDriverMetric>> loadBalancerMetrics, Set<StackDriverMetric> vmInstanceMetrics, String guid) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.STACK_DRIVER, fromTime, toTime);
    this.loadBalancerMetrics = loadBalancerMetrics;
    this.vmInstanceMetrics = vmInstanceMetrics;
  }
}
