package software.wings.delegatetasks.validation.capabilitycheck;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;

import software.wings.delegatetasks.validation.capabilities.SmbConnectionCapability;
import software.wings.service.impl.SmbHelperService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmbConnectionCapabilityCheck implements CapabilityCheck {
  @Inject private SmbHelperService smbHelperService;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SmbConnectionCapability capability = (SmbConnectionCapability) delegateCapability;
    try {
      String host = smbHelperService.getSMBConnectionHost(capability.getSmbUrl());
      boolean validated = smbHelperService.isConnectibleSOBServer(host);
      return CapabilityResponse.builder().validated(validated).delegateCapability(capability).build();
    } catch (Exception exception) {
      log.error("Cannot Connect to SMB xxxxxxxx {}, Reason: {}", capability.getSmbUrl(), exception.getMessage());
      return CapabilityResponse.builder().validated(false).delegateCapability(capability).build();
    }
  }
}
