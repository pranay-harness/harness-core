package software.wings.service.impl.instana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InstanaSetupTestNodeData extends SetupTestNodeData {
  private List<String> metrics;
  private String query;
  @Builder
  private InstanaSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      String guid, List<String> metrics, String query) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.APP_DYNAMICS, fromTime, toTime);
    this.metrics = metrics;
    this.query = query;
  }
}
