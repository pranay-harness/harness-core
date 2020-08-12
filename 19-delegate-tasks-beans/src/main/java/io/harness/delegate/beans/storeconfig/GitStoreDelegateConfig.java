package io.harness.delegate.beans.storeconfig;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

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
