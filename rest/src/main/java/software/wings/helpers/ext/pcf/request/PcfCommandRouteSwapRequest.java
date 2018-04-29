package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.PcfConfig;

import java.util.List;

/**
 * This class contains all required data for PCFCommandTask.SETUP to perform setup task
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PcfCommandRouteSwapRequest extends PcfCommandRequest {
  /**
   * releasePrefixName is (appId_serviceId_envId), while creating new version of app,
   * we will add 1 to mosr recent version deployed,
   * so actual app name will be appId_serviceId_envId__version
   */
  private String releaseName;
  private List<String> routeMaps;
  private List<String> tempRouteMaps;
  private List<String> appsToBeDownSized;

  @Builder
  public PcfCommandRouteSwapRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      String releaseName, List<String> routeMaps, List<String> tempRouteMaps, Integer timeoutIntervalInMin,
      boolean isBlueGreenDeployment, List<String> appsToBeDownSized) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, isBlueGreenDeployment);
    this.releaseName = releaseName;
    this.routeMaps = routeMaps;
    this.tempRouteMaps = tempRouteMaps;
    this.appsToBeDownSized = appsToBeDownSized;
  }
}
