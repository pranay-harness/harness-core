package io.harness.delegate.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.task.TaskParameters;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class SSHTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  SSHKeySpecDTO sshKeySpec;
  String host;
  List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(SocketConnectivityExecutionCapability.builder()
                                         .hostName(host)
                                         .port(String.valueOf(sshKeySpec.getPort()))
                                         .build());
  }
}
