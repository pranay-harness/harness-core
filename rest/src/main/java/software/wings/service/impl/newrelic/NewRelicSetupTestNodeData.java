package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;

/**
 * Created by rsingh on 8/3/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class NewRelicSetupTestNodeData extends SetupTestNodeData {
  private long newRelicAppId;

  @Builder
  public NewRelicSetupTestNodeData(String appId, String settingId, String instanceName, InstanceElement instanceElement,
      String hostExpression, String workflowId, long fromTime, long toTime, long newRelicAppId) {
    super(appId, settingId, instanceName, instanceElement, hostExpression, workflowId, fromTime, toTime);
    this.newRelicAppId = newRelicAppId;
  }
}
