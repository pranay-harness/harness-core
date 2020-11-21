package software.wings.delegatetasks.validation.capabilitycheck;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;

import software.wings.delegatetasks.validation.capabilities.SftpCapability;
import software.wings.service.impl.SftpHelperService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SftpCapabilityCheck implements CapabilityCheck {
  @Inject private SftpHelperService sftpHelperService;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SftpCapability sftpCapability = (SftpCapability) delegateCapability;
    String connectionHost = sftpHelperService.getSFTPConnectionHost(sftpCapability.getSftpUrl());
    return CapabilityResponse.builder()
        .delegateCapability(sftpCapability)
        .validated(sftpHelperService.isConnectibleSFTPServer(connectionHost))
        .build();
  }
}
