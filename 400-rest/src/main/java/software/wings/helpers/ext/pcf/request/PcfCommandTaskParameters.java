package software.wings.helpers.ext.pcf.request;

import static io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.PcfAutoScalarCapability;
import io.harness.delegate.beans.executioncapability.PcfConnectivityCapability;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class PcfCommandTaskParameters implements ExecutionCapabilityDemander {
  private PcfCommandRequest pcfCommandRequest;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities = new ArrayList<>();
    capabilities.add(
        PcfConnectivityCapability.builder().endpointUrl(pcfCommandRequest.getPcfConfig().getEndpointUrl()).build());
    capabilities.addAll(fetchExecutionCapabilitiesForEncryptedDataDetails(encryptedDataDetails, maskingEvaluator));
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
