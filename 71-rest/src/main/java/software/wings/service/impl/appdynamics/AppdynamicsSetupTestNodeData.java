package software.wings.service.impl.appdynamics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;

/**
 * Created by rsingh on 8/3/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AppdynamicsSetupTestNodeData extends SetupTestNodeData {
  private long applicationId;
  private long tierId;

  @Builder
  private AppdynamicsSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      long applicationId, long tierId) {
    super(
        appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, fromTime, toTime);
    this.applicationId = applicationId;
    this.tierId = tierId;
  }
}
