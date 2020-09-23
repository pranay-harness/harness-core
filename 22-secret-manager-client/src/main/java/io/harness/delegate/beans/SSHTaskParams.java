package io.harness.delegate.beans;

import io.harness.delegate.task.TaskParameters;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SSHTaskParams implements TaskParameters {
  SSHKeySpecDTO sshKeySpec;
  String host;
  List<EncryptedDataDetail> encryptionDetails;
}