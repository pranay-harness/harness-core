package io.harness.delegate.chartmuseum;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.chartmuseum.ChartMuseumClientHelper;
import io.harness.chartmuseum.ChartMuseumServer;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.utils.FieldWithPlainTextOrSecretValueHelper;

import com.google.inject.Inject;

@OwnedBy(CDP)
public class NGChartMuseumServiceImpl implements NGChartMuseumService {
  @Inject private ChartMuseumClientHelper clientHelper;

  @Override
  public ChartMuseumServer startChartMuseumServer(StoreDelegateConfig storeDelegateConfig, String resourceDirectory)
      throws Exception {
    switch (storeDelegateConfig.getType()) {
      case S3_HELM:
        S3HelmStoreDelegateConfig s3StoreDelegateConfig = (S3HelmStoreDelegateConfig) storeDelegateConfig;
        AwsConnectorDTO awsConnector = s3StoreDelegateConfig.getAwsConnector();
        boolean inheritFromDelegate = false;
        boolean irsa = false;
        char[] accessKey = null;
        char[] secretKey = null;

        switch (awsConnector.getCredential().getAwsCredentialType()) {
          case MANUAL_CREDENTIALS:
            AwsManualConfigSpecDTO config = (AwsManualConfigSpecDTO) awsConnector.getCredential().getConfig();
            String accessKeyString = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
                config.getAccessKey(), config.getAccessKeyRef());
            accessKey = accessKeyString == null ? null : accessKeyString.toCharArray();
            secretKey = config.getSecretKeyRef().getDecryptedValue();
            break;

          case INHERIT_FROM_DELEGATE:
            inheritFromDelegate = true;
            break;

          case IRSA:
            irsa = true;
            break;

          default:
            throw new UnsupportedOperationException(
                format("Credentials type %s are not supported", awsConnector.getCredential().getAwsCredentialType()));
        }

        return clientHelper.startS3ChartMuseumServer(s3StoreDelegateConfig.getBucketName(),
            s3StoreDelegateConfig.getFolderPath(), s3StoreDelegateConfig.getRegion(), inheritFromDelegate, accessKey,
            secretKey, irsa);

      case GCS_HELM:
        GcsHelmStoreDelegateConfig gcsHelmStoreDelegateConfig = (GcsHelmStoreDelegateConfig) storeDelegateConfig;
        GcpConnectorCredentialDTO credentials = gcsHelmStoreDelegateConfig.getGcpConnector().getCredential();
        char[] serviceAccountKey = null;

        if (GcpCredentialType.MANUAL_CREDENTIALS == credentials.getGcpCredentialType()) {
          GcpManualDetailsDTO manualCredentials = (GcpManualDetailsDTO) credentials.getConfig();
          serviceAccountKey = manualCredentials.getSecretKeyRef().getDecryptedValue();
        }

        return clientHelper.startGCSChartMuseumServer(gcsHelmStoreDelegateConfig.getBucketName(),
            gcsHelmStoreDelegateConfig.getFolderPath(), serviceAccountKey, resourceDirectory);

      default:
        throw new UnsupportedOperationException(
            format("Manifest store config type: [%s]", storeDelegateConfig.getType()));
    }
  }

  @Override
  public void stopChartMuseumServer(ChartMuseumServer chartMuseumServer) {
    if (chartMuseumServer != null) {
      clientHelper.stopChartMuseumServer(chartMuseumServer.getStartedProcess());
    }
  }
}
