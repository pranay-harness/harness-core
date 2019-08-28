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
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StackDriverSetupTestNodeData extends SetupTestNodeData {
  private boolean isLogConfiguration;

  private Map<String, List<StackDriverMetric>> loadBalancerMetrics = new HashMap<>();

  private Set<StackDriverMetric> podMetrics = new HashSet<>();

  private String query;

  private String hostnameField;

  private String messageField;

  @Builder
  public StackDriverSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      Map<String, List<StackDriverMetric>> loadBalancerMetrics, Set<StackDriverMetric> podMetrics, String guid,
      String query, String hostnameField, String messageField, boolean isLogConfiguration) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.STACK_DRIVER, fromTime, toTime);
    this.loadBalancerMetrics = loadBalancerMetrics;
    this.podMetrics = podMetrics;
    this.query = query;
    this.hostnameField = hostnameField;
    this.messageField = messageField;
    this.isLogConfiguration = isLogConfiguration;
  }
}
