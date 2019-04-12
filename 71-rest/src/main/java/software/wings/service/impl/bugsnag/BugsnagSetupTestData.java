package software.wings.service.impl.bugsnag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

/**
 * Created by Pranjal on 04/09/2019
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BugsnagSetupTestData extends SetupTestNodeData {
  private String orgId;
  private String projectId;
  private String releaseStage;
  private String query;
  private boolean browserApplication;

  @Builder
  public BugsnagSetupTestData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, String guid, StateType stateType,
      long fromTime, long toTime, String orgId, String projectId, String releaseStage, String query,
      boolean browserApplication) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid, stateType,
        fromTime, toTime);
    this.orgId = orgId;
    this.projectId = projectId;
    this.releaseStage = releaseStage;
    this.query = query;
    this.browserApplication = browserApplication;
  }
}
