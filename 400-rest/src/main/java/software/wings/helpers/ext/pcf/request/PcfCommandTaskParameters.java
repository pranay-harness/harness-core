package software.wings.helpers.ext.pcf.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.delegatetasks.validation.capabilities.PcfAutoScalarCapability;
import software.wings.delegatetasks.validation.capabilities.PcfConnectivityCapability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PcfCommandTaskParameters implements ExecutionCapabilityDemander {
  private PcfCommandRequest pcfCommandRequest;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities = new ArrayList<>();
    capabilities.add(PcfConnectivityCapability.builder()
                         .limitPcfThreads(pcfCommandRequest.isLimitPcfThreads())
                         .ignorePcfConnectionContextCache(pcfCommandRequest.isIgnorePcfConnectionContextCache())
                         .pcfConfig(pcfCommandRequest.getPcfConfig())
                         .encryptionDetails(encryptedDataDetails)
                         .build());
    if (pcfCommandRequest.isUseCfCLI() || needToCheckAppAutoscalarPluginInstall()) {
      capabilities.add(ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(
          "PCF", Arrays.asList("/bin/sh", "-c", "cf --version")));
    }

    if (needToCheckAppAutoscalarPluginInstall()) {
      capabilities.add(PcfAutoScalarCapability.builder().build());
    }
    return capabilities;
  }

  private boolean needToCheckAppAutoscalarPluginInstall() {
    boolean checkAppAutoscalarPluginInstall = false;
    if (pcfCommandRequest instanceof PcfCommandSetupRequest || pcfCommandRequest instanceof PcfCommandDeployRequest
        || pcfCommandRequest instanceof PcfCommandRollbackRequest
        || pcfCommandRequest instanceof PcfCommandRouteUpdateRequest) {
      checkAppAutoscalarPluginInstall = pcfCommandRequest.isUseAppAutoscalar();
    }

    return checkAppAutoscalarPluginInstall;
  }
}
