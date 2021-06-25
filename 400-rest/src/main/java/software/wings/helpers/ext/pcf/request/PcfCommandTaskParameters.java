package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.PcfAutoScalarCapability;
import io.harness.delegate.beans.executioncapability.PcfConnectivityCapability;
import io.harness.delegate.beans.executioncapability.PcfInstallationCapability;
import io.harness.expression.ExpressionEvaluator;
import io.harness.pcf.model.CfCliVersion;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
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
      CfCliVersion cfCliVersion = pcfCommandRequest.getCfCliVersion();
      capabilities.add(PcfInstallationCapability.builder()
                           .criteria(format("CF CLI version: %s is installed", cfCliVersion))
                           .version(cfCliVersion)
                           .build());
    }

    if (needToCheckAppAutoscalarPluginInstall()) {
      CfCliVersion cfCliVersion = pcfCommandRequest.getCfCliVersion();
      capabilities.add(PcfAutoScalarCapability.builder()
                           .version(cfCliVersion)
                           .criteria("App Autoscaler plugin is installed")
                           .build());
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
