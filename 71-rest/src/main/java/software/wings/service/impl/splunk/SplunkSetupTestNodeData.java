package software.wings.service.impl.splunk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import javax.validation.constraints.NotNull;

/**
 * Created by Pranjal on 08/31/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SplunkSetupTestNodeData extends SetupTestNodeData {
  @NotNull private String query;
  private String hostNameField;
  private boolean isAdvancedQuery;

  @Builder
  public SplunkSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      String query, String hostNameField, String guid, boolean isAdvancedQuery) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.SPLUNKV2, fromTime, toTime);
    this.query = query;
    this.hostNameField = hostNameField;
    this.isAdvancedQuery = isAdvancedQuery;
  }
}
