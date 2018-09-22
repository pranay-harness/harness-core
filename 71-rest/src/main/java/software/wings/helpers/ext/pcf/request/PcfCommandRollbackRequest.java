package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

import java.util.List;

/**
 * This class contains all required data for PCFCommandTask.DEPLOY to perform setup task
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PcfCommandRollbackRequest extends PcfCommandRequest {
  private List<PcfServiceData> instanceData;
  private List<String> routeMaps;
  private List<String> tempRouteMaps;
  private ResizeStrategy resizeStrategy;
  private List<PcfAppSetupTimeDetails> appsToBeDownSized;
  private PcfAppSetupTimeDetails newApplicationDetails;
  private boolean isStandardBlueGreenWorkflow;

  @Builder
  public PcfCommandRollbackRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      List<PcfServiceData> instanceData, ResizeStrategy resizeStrategy, List<String> routeMaps,
      List<String> tempRouteMaps, Integer timeoutIntervalInMin, List<PcfAppSetupTimeDetails> appsToBeDownSized,
      PcfAppSetupTimeDetails newApplicationDetails, boolean isStandardBlueGreenWorkflow) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin);
    this.instanceData = instanceData;
    this.resizeStrategy = resizeStrategy;
    this.routeMaps = routeMaps;
    this.tempRouteMaps = tempRouteMaps;
    this.appsToBeDownSized = appsToBeDownSized;
    this.newApplicationDetails = newApplicationDetails;
    this.isStandardBlueGreenWorkflow = isStandardBlueGreenWorkflow;
  }
}
