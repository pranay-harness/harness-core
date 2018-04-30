package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.PcfConfig;

import java.util.List;

/**
 * This class contains all required data for PCFCommandTask.SETUP to perform setup task
 */
@Data
public class PcfCommandRouteUpdateRequest extends PcfCommandRequest {
  private List<String> routeMaps;
  private List<String> appsToBeUpdated;
  private boolean isMapRoutesOperation;

  @Builder
  public PcfCommandRouteUpdateRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      List<String> routeMaps, Integer timeoutIntervalInMin, List<String> appsToBeUpdated,
      boolean isMapRoutesOperation) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin);
    this.routeMaps = routeMaps;
    this.appsToBeUpdated = appsToBeUpdated;
    this.isMapRoutesOperation = isMapRoutesOperation;
  }
}
