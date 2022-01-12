/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.chartmuseum;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.chartmuseum.ChartMuseumClientHelper;
import io.harness.chartmuseum.ChartMuseumServer;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zeroturnaround.exec.StartedProcess;

@OwnedBy(CDP)
public class NGChartMuseumServiceImplTest extends CategoryTest {
  @Mock private ChartMuseumClientHelper clientHelper;
  @InjectMocks private NGChartMuseumServiceImpl ngChartMuseumService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartChartMuseumServerUsingAwsS3InheritFromDelegate() throws Exception {
    testStartChartMuseumServerUsingAwsS3(AwsCredentialDTO.builder()
                                             .awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
                                             .config(AwsInheritFromDelegateSpecDTO.builder().build())
                                             .build(),
        true, null, null);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartChartMuseumServerUsingAwsS3ManualCredentials() throws Exception {
    final char[] accessKey = "access-key".toCharArray();
    final char[] secretKey = "secret-key".toCharArray();

    testStartChartMuseumServerUsingAwsS3(
        AwsCredentialDTO.builder()
            .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
            .config(AwsManualConfigSpecDTO.builder()
                        .accessKeyRef(SecretRefData.builder().decryptedValue(accessKey).build())
                        .secretKeyRef(SecretRefData.builder().decryptedValue(secretKey).build())
                        .build())
            .build(),
        false, accessKey, secretKey);
  }

  private void testStartChartMuseumServerUsingAwsS3(
      AwsCredentialDTO credentials, boolean inheritFromDelegate, char[] accessKey, char[] secretKey) throws Exception {
    final String bucketName = "bucketName";
    final String region = "region";
    final String folderPath = "folderPath";

    S3HelmStoreDelegateConfig s3StoreDelegateConfig =
        S3HelmStoreDelegateConfig.builder()
            .bucketName(bucketName)
            .region(region)
            .folderPath(folderPath)
            .awsConnector(AwsConnectorDTO.builder().credential(credentials).build())
            .useLatestChartMuseumVersion(true)
            .build();

    ngChartMuseumService.startChartMuseumServer(s3StoreDelegateConfig, "resources");
    verify(clientHelper, times(1))
        .startS3ChartMuseumServer(
            bucketName, folderPath, region, inheritFromDelegate, accessKey, secretKey, false, true);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartChartMuseumServerUsingGCS() throws Exception {
    final char[] serviceAccountKey = "service account key content".toCharArray();
    final String bucketName = "bucketName";
    final String folderPath = "folderPath";
    final String resourcesDir = "resources";

    GcsHelmStoreDelegateConfig gcsHelmStoreDelegateConfig =
        GcsHelmStoreDelegateConfig.builder()
            .bucketName(bucketName)
            .folderPath(folderPath)
            .gcpConnector(
                GcpConnectorDTO.builder()
                    .credential(
                        GcpConnectorCredentialDTO.builder()
                            .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                            .config(GcpManualDetailsDTO.builder()
                                        .secretKeyRef(SecretRefData.builder().decryptedValue(serviceAccountKey).build())
                                        .build())
                            .build())
                    .build())
            .useLatestChartMuseumVersion(true)
            .build();

    ngChartMuseumService.startChartMuseumServer(gcsHelmStoreDelegateConfig, "resources");
    verify(clientHelper, times(1))
        .startGCSChartMuseumServer(bucketName, folderPath, serviceAccountKey, resourcesDir, true);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartChartmuseumServerUnsuportedStoreType() {
    GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder().build();

    assertThatThrownBy(() -> ngChartMuseumService.startChartMuseumServer(gitStoreDelegateConfig, "resoources"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStopChartMuseumServer() {
    StartedProcess startedProcess = mock(StartedProcess.class);
    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().startedProcess(startedProcess).build();

    ngChartMuseumService.stopChartMuseumServer(chartMuseumServer);
    verify(clientHelper).stopChartMuseumServer(startedProcess);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStopChartMuseumServerNull() {
    ngChartMuseumService.stopChartMuseumServer(null);
    verify(clientHelper, never()).stopChartMuseumServer(any(StartedProcess.class));
  }
}
