package io.harness.delegate.beans.storeconfig;

import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
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

  GitConfigDTO gitConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
}
