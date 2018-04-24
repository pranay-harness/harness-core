package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;

import java.util.List;

/**
 * This class contains all required data for PCFCommandTask.DEPLOY to perform setup task
 */
@Data
public class PcfCommandRollbackRequest extends PcfCommandRequest {
  private List<PcfServiceData> instanceData;
  private List<String> routeMaps;
  private ResizeStrategy resizeStrategy;

  @Builder
  public PcfCommandRollbackRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      List<PcfServiceData> instanceData, ResizeStrategy resizeStrategy, List<String> routeMaps,
      Integer timeoutIntervalInMin) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin);
    this.instanceData = instanceData;
    this.resizeStrategy = resizeStrategy;
    this.routeMaps = routeMaps;
  }
}
