package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.PcfConfig;

/**
 * This class contains all required data for PCFCommandTask.SETUP to perform setup task
 */
@Data
public class PcfCommandRouteSwapRequest extends PcfCommandRequest {
  /**
   * releasePrefixName is (appId_serviceId_envId), while creating new version of app,
   * we will add 1 to mosr recent version deployed,
   * so actual app name will be appId_serviceId_envId__version
   */
  private String releaseName;
  private String routeMap;

  @Builder
  public PcfCommandRouteSwapRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      String releaseName, String routeMap, Integer timeoutIntervalInMin) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin);
    this.releaseName = releaseName;
    this.routeMap = routeMap;
  }
}
