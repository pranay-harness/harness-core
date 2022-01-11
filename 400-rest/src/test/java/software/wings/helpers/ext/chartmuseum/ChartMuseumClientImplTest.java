/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.chartmuseum;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACHYUTH;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.chartmuseum.ChartMuseumClientHelper;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
@TargetModule(HarnessModule._960_API_SERVICES)
@BreakDependencyOn("software.wings.beans.AwsConfig")
@BreakDependencyOn("software.wings.beans.GcpConfig")
@BreakDependencyOn("software.wings.beans.settings.helm.AmazonS3HelmRepoConfig")
@BreakDependencyOn("software.wings.beans.settings.helm.GCSHelmRepoConfig")
@BreakDependencyOn("software.wings.beans.settings.helm.HelmRepoConfig")
@BreakDependencyOn("software.wings.settings.SettingValue")
public class ChartMuseumClientImplTest extends WingsBaseTest {
  @Mock ChartMuseumClientHelper clientHelper;
  @InjectMocks private ChartMuseumClientImpl chartMuseumClient;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartChartMuseumServerAwsS3() throws Exception {
    final char[] accessKey = "access-key".toCharArray();
    final char[] secretKey = "secret-key".toCharArray();
    final String bucketName = "bucket-name";
    final String region = "us-west1";
    final String basePath = "base-path";
    AwsConfig awsConfig =
        AwsConfig.builder().useEc2IamCredentials(true).useIRSA(true).accessKey(accessKey).secretKey(secretKey).build();
    AmazonS3HelmRepoConfig s3HelmRepoConfig =
        AmazonS3HelmRepoConfig.builder().bucketName(bucketName).region(region).build();

    chartMuseumClient.startChartMuseumServer(s3HelmRepoConfig, awsConfig, "resource-directory", basePath, false);

    verify(clientHelper, times(1))
        .startS3ChartMuseumServer(bucketName, basePath, region, true, accessKey, secretKey, true, false);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartChartMuseumServerGCS() throws Exception {
    final char[] serviceAccountKey = "service-account-key".toCharArray();
    final String bucketName = "bucket-name";
    final String basePath = "base-path";
    final String resourceDirectory = "resource-directory";
    GcpConfig gcpConfig = GcpConfig.builder().serviceAccountKeyFileContent(serviceAccountKey).build();
    GCSHelmRepoConfig helmRepoConfig = GCSHelmRepoConfig.builder().bucketName(bucketName).build();

    chartMuseumClient.startChartMuseumServer(helmRepoConfig, gcpConfig, resourceDirectory, basePath, false);
    verify(clientHelper, times(1))
        .startGCSChartMuseumServer(bucketName, basePath, serviceAccountKey, resourceDirectory, false);
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testChartMuseumServerAwsS3IRSA() throws Exception {
    final String bucketName = "bucket-name";
    final String region = "us-west1";
    final String basePath = "base-path";
    AwsConfig awsConfig = AwsConfig.builder().useEc2IamCredentials(false).useIRSA(true).build();
    AmazonS3HelmRepoConfig s3HelmRepoConfig =
        AmazonS3HelmRepoConfig.builder().bucketName(bucketName).region(region).build();

    chartMuseumClient.startChartMuseumServer(s3HelmRepoConfig, awsConfig, "resource-directory", basePath, false);

    verify(clientHelper, times(1))
        .startS3ChartMuseumServer(bucketName, basePath, region, false, null, null, true, false);
  }
}
