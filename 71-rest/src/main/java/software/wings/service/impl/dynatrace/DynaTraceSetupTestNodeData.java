package software.wings.service.impl.dynatrace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

/**
 * Created by Pranjal on 09/12/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DynaTraceSetupTestNodeData extends SetupTestNodeData {
  private String serviceMethods;
  private String serviceEntityId;

  @Builder
  public DynaTraceSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      String serviceMethods, String guid, String serviceEntityId) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.DYNA_TRACE, fromTime, toTime);
    this.serviceMethods = serviceMethods;
    this.serviceEntityId = serviceEntityId;
  }
}
