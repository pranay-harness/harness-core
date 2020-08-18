package io.harness.azure.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureUserAuthVMInstanceData {
  private String vmssAuthType;
  private String userName;
  private String password;
  private String sshPublicKey;
}
