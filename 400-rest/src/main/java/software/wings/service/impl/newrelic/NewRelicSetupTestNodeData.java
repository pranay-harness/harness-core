package software.wings.service.impl.newrelic;

import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 8/3/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NewRelicSetupTestNodeData extends SetupTestNodeData {
  private long newRelicAppId;

  @Builder
  public NewRelicSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      Instance instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      long newRelicAppId, String guid) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.NEW_RELIC.name(), fromTime, toTime);
    this.newRelicAppId = newRelicAppId;
  }
}
