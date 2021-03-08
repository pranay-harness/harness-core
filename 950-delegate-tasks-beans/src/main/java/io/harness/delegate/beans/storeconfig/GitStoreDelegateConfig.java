package io.harness.delegate.beans.storeconfig;

import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class GitStoreDelegateConfig implements StoreDelegateConfig {
  String branch;
  String commitId;
  String connectorName;
  @Singular List<String> paths;
  FetchType fetchType;

  ScmConnector gitConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  SSHKeySpecDTO sshKeySpecDTO;

  @Override
  public StoreDelegateConfigType getType() {
    return StoreDelegateConfigType.GIT;
  }
}
