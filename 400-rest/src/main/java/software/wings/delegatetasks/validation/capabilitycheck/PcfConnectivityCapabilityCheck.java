package software.wings.delegatetasks.validation.capabilitycheck;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.network.Http;

import software.wings.delegatetasks.validation.capabilities.PcfConnectivityCapability;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfConnectivityCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfConnectivityCapability pcfConnectivityCapability = (PcfConnectivityCapability) delegateCapability;
    try {
      boolean httpEndpointConnectable = isEndpointConnectable(pcfConnectivityCapability, "http://");
      boolean httpsEndpointConnectable = isEndpointConnectable(pcfConnectivityCapability, "https://");

      return CapabilityResponse.builder()
          .delegateCapability(pcfConnectivityCapability)
          .validated(httpEndpointConnectable || httpsEndpointConnectable)
          .build();
    } catch (Exception e) {
      log.error("Failed to connect, RepoUrl: {}", pcfConnectivityCapability.getEndpointUrl());
      return CapabilityResponse.builder().delegateCapability(pcfConnectivityCapability).validated(false).build();
    }
  }

  boolean isEndpointConnectable(PcfConnectivityCapability pcfConnectivityCapability, String urlScheme) {
    return Http.connectableHttpUrl(urlScheme + pcfConnectivityCapability.getEndpointUrl());
  }
}
