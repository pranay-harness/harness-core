package software.wings.service.impl.sumo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;

/**
 * Created by Pranjal on 08/23/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SumoLogicSetupTestNodedata extends SetupTestNodeData {
  private String query;
  private String hostNameField;

  @Builder
  SumoLogicSetupTestNodedata(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      String query, String hostNameField) {
    super(
        appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, fromTime, toTime);
    this.query = query;
    this.hostNameField = hostNameField;
  }
}
