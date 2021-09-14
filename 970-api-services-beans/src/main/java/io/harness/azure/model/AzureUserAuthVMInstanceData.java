/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
