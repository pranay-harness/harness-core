package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FileData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.PcfConnectivityCapability;
import io.harness.delegate.beans.executioncapability.PcfInstallationCapability;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.pcf.model.PcfCliVersion;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.PcfConfig;

import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class PcfRunPluginCommandRequest
    extends PcfCommandRequest implements TaskParameters, ExecutionCapabilityDemander, ActivityAccess {
  @Expression(ALLOW_SECRETS) private String renderedScriptString;
  private List<String> filePathsInScript;
  private List<FileData> fileDataList;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String repoRoot;
  private boolean useCfCLI7;

  @Builder
  public PcfRunPluginCommandRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, boolean useCLIForPcfAppCreation, boolean enforceSslValidation,
      boolean useAppAutoscalar, String renderedScriptString, List<String> filePathsInScript,
      List<FileData> fileDataList, List<EncryptedDataDetail> encryptedDataDetails, String repoRoot,
      boolean limitPcfThreads, boolean ignorePcfConnectionContextCache, boolean useCfCLI7) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCLIForPcfAppCreation, enforceSslValidation, useAppAutoscalar,
        limitPcfThreads, ignorePcfConnectionContextCache, useCfCLI7);
    this.renderedScriptString = renderedScriptString;
    this.filePathsInScript = filePathsInScript;
    this.fileDataList = fileDataList;
    this.encryptedDataDetails = encryptedDataDetails;
    this.repoRoot = repoRoot;
    this.useCfCLI7 = useCfCLI7;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    PcfCliVersion cfCliVersion = useCfCLI7 ? PcfCliVersion.V7 : PcfCliVersion.V6;
    return Arrays.asList(PcfConnectivityCapability.builder().endpointUrl(getPcfConfig().getEndpointUrl()).build(),
        PcfInstallationCapability.builder()
            .criteria(format("CF CLI version: %s is installed", cfCliVersion))
            .version(cfCliVersion)
            .build());
  }
}
