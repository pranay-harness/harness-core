package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pcf.model.CfCliVersion;

import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This class contains all required data for PCFCommandTask.DEPLOY to perform setup task
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
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
      PcfAppSetupTimeDetails newApplicationDetails, boolean isStandardBlueGreenWorkflow, boolean useCfCLI,
      boolean useAppAutoscalar, boolean enforceSslValidation, boolean limitPcfThreads,
      boolean ignorePcfConnectionContextCache, CfCliVersion cfCliVersion) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCfCLI, enforceSslValidation, useAppAutoscalar, limitPcfThreads,
        ignorePcfConnectionContextCache, cfCliVersion);
    this.instanceData = instanceData;
    this.resizeStrategy = resizeStrategy;
    this.routeMaps = routeMaps;
    this.tempRouteMaps = tempRouteMaps;
    this.appsToBeDownSized = appsToBeDownSized;
    this.newApplicationDetails = newApplicationDetails;
    this.isStandardBlueGreenWorkflow = isStandardBlueGreenWorkflow;
  }
}
