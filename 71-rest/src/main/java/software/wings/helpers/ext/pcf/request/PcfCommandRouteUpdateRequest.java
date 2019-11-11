package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.PcfConfig;

/**
 * This class contains all required data for PCFCommandTask.SETUP to perform setup task
 */
@Data
public class PcfCommandRouteUpdateRequest extends PcfCommandRequest {
  private PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData;

  @Builder
  public PcfCommandRouteUpdateRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData,
      boolean useCLIForPcfAppCreation, boolean useAppAutoscalar, boolean enforceSslValidation) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCLIForPcfAppCreation, enforceSslValidation, useAppAutoscalar);
    this.pcfRouteUpdateConfigData = pcfRouteUpdateConfigData;
  }
}
