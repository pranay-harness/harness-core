package io.harness.delegate.beans.storeconfig;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class GcsHelmStoreDelegateConfig implements StoreDelegateConfig {
  String repoName;
  String repoDisplayName;
  String bucketName;
  String folderPath;
  boolean useLatestChartMuseumVersion;

  GcpConnectorDTO gcpConnector;
  List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public StoreDelegateConfigType getType() {
    return StoreDelegateConfigType.GCS_HELM;
  }
}
