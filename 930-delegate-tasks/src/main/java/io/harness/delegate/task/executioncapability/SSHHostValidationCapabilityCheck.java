package io.harness.delegate.task.executioncapability;

import static java.time.Duration.ofSeconds;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.SshHostValidationParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.CapabilityResponse.CapabilityResponseBuilder;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SSHHostValidationCapability;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SSHHostValidationCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SSHHostValidationCapability capability = (SSHHostValidationCapability) delegateCapability;
    CapabilityResponseBuilder capabilityResponseBuilder = CapabilityResponse.builder().delegateCapability(capability);

    return capabilityResponseBuilder.validated(checkConnectivity(capability.getHost(), capability.getPort())).build();
  }

  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.SSH_HOST_VALIDATION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }

    SshHostValidationParameters details = parameters.getSshHostValidationParameters();

    return builder
        .permissionResult(checkConnectivity(details.getHostName(), details.getPort()) ? PermissionResult.ALLOWED
                                                                                      : PermissionResult.DENIED)
        .build();
  }

  static boolean checkConnectivity(String host, int port) {
    try {
      JSch jsch = new JSch();
      Session session = jsch.getSession("username_placeholder", host, port);
      session.connect((int) ofSeconds(15L).toMillis());
      session.disconnect();
      return true;
    } catch (JSchException jschException) {
      if (jschException.getMessage().contains("Auth fail")) {
        // if we had the correct password, it would have connected, so we mark it as true.
        return true;
      } else {
        return false;
      }
    }
  }
}
