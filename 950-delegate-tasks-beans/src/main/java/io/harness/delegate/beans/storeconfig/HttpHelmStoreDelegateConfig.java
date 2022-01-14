package io.harness.delegate.beans.storeconfig;

import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpHelmStoreDelegateConfig implements StoreDelegateConfig {
  String repoName;
  String repoDisplayName;
  HttpHelmConnectorDTO httpHelmConnector;
  List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public StoreDelegateConfigType getType() {
    return StoreDelegateConfigType.HTTP_HELM;
  }
}
