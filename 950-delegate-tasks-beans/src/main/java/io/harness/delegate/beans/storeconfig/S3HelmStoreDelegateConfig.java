package io.harness.delegate.beans.storeconfig;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class S3HelmStoreDelegateConfig implements StoreDelegateConfig {
  String repoName;
  String repoDisplayName;
  String bucketName;
  String region;
  String folderPath;
  boolean useLatestChartMuseumVersion;

  AwsConnectorDTO awsConnector;
  List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public StoreDelegateConfigType getType() {
    return StoreDelegateConfigType.S3_HELM;
  }
}
