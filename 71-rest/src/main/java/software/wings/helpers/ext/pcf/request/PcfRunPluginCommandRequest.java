package software.wings.helpers.ext.pcf.request;

import io.harness.beans.FileData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.expression.Expression;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.PcfConfig;
import software.wings.delegatetasks.validation.capabilities.PcfConnectivityCapability;

import java.util.Arrays;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class PcfRunPluginCommandRequest
    extends PcfCommandRequest implements TaskParameters, ExecutionCapabilityDemander {
  @Expression private String renderedScriptString;
  private List<String> filePathsInScript;
  private List<FileData> fileDataList;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String repoRoot;

  @Builder
  public PcfRunPluginCommandRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, boolean useCLIForPcfAppCreation, boolean enforceSslValidation,
      boolean useAppAutoscalar, String renderedScriptString, List<String> filePathsInScript,
      List<FileData> fileDataList, List<EncryptedDataDetail> encryptedDataDetails, String repoRoot) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCLIForPcfAppCreation, enforceSslValidation, useAppAutoscalar);
    this.renderedScriptString = renderedScriptString;
    this.filePathsInScript = filePathsInScript;
    this.fileDataList = fileDataList;
    this.encryptedDataDetails = encryptedDataDetails;
    this.repoRoot = repoRoot;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        PcfConnectivityCapability.builder().pcfConfig(getPcfConfig()).encryptionDetails(encryptedDataDetails).build(),
        ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(
            "PCF", Arrays.asList("/bin/sh", "-c", "cf --version")));
  }
}
