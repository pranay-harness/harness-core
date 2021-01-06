package software.wings.helpers.ext.pcf.request;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.beans.FileData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.PcfConfig;
import software.wings.delegatetasks.validation.capabilities.PcfConnectivityCapability;

import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PcfRunPluginCommandRequest
    extends PcfCommandRequest implements TaskParameters, ExecutionCapabilityDemander, ActivityAccess {
  @Expression(ALLOW_SECRETS) private String renderedScriptString;
  private List<String> filePathsInScript;
  private List<FileData> fileDataList;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String repoRoot;

  @Builder
  public PcfRunPluginCommandRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, boolean useCLIForPcfAppCreation, boolean enforceSslValidation,
      boolean useAppAutoscalar, String renderedScriptString, List<String> filePathsInScript,
      List<FileData> fileDataList, List<EncryptedDataDetail> encryptedDataDetails, String repoRoot,
      boolean limitPcfThreads, boolean ignorePcfConnectionContextCache) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCLIForPcfAppCreation, enforceSslValidation, useAppAutoscalar,
        limitPcfThreads, ignorePcfConnectionContextCache);
    this.renderedScriptString = renderedScriptString;
    this.filePathsInScript = filePathsInScript;
    this.fileDataList = fileDataList;
    this.encryptedDataDetails = encryptedDataDetails;
    this.repoRoot = repoRoot;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(PcfConnectivityCapability.builder().endpointUrl(getPcfConfig().getEndpointUrl()).build(),
        ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(
            "PCF", Arrays.asList("/bin/sh", "-c", "cf --version")));
  }
}
