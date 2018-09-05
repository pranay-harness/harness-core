package software.wings.service.impl.splunk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;

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

  @Builder
  public SplunkSetupTestNodeData(String appId, String settingId, String instanceName, InstanceElement instanceElement,
      String hostExpression, String workflowId, long fromTime, long toTime, String query, String hostNameField) {
    super(appId, settingId, instanceName, instanceElement, hostExpression, workflowId, fromTime, toTime);
    this.query = query;
    this.hostNameField = hostNameField;
  }
}
