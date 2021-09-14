/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.storeconfig;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class GitStoreDelegateConfig implements StoreDelegateConfig {
  String branch;
  String commitId;
  String connectorName;
  @Singular List<String> paths;
  FetchType fetchType;

  ScmConnector gitConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  SSHKeySpecDTO sshKeySpecDTO;
  String manifestType;
  String manifestId;

  @Override
  public StoreDelegateConfigType getType() {
    return StoreDelegateConfigType.GIT;
  }
}
