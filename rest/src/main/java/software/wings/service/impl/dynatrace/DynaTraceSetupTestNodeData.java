package software.wings.service.impl.dynatrace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;

import java.util.Set;

/**
 * Created by Pranjal on 09/12/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DynaTraceSetupTestNodeData extends SetupTestNodeData {
  private Set<String> serviceMethods;

  @Builder
  public DynaTraceSetupTestNodeData(String appId, String settingId, String instanceName,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      Set<String> serviceMethods) {
    super(appId, settingId, instanceName, instanceElement, hostExpression, workflowId, fromTime, toTime);
    this.serviceMethods = serviceMethods;
  }
}
