package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pcf.model.CfCliVersion;

import software.wings.beans.PcfConfig;

import lombok.Builder;
import lombok.Data;

/**
 * This class contains all required data for PCFCommandTask.SETUP to perform setup task
 */
@Data
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class PcfCommandRouteUpdateRequest extends PcfCommandRequest {
  private PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData;

  @Builder
  public PcfCommandRouteUpdateRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, PcfRouteUpdateRequestConfigData pcfRouteUpdateConfigData, boolean useCfCLI,
      boolean useAppAutoscalar, boolean enforceSslValidation, boolean limitPcfThreads,
      boolean ignorePcfConnectionContextCache, CfCliVersion cfCliVersion) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCfCLI, enforceSslValidation, useAppAutoscalar, limitPcfThreads,
        ignorePcfConnectionContextCache, cfCliVersion);
    this.pcfRouteUpdateConfigData = pcfRouteUpdateConfigData;
  }
}
