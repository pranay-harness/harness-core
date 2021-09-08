package software.wings.helpers.ext.chartmuseum;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    chartMuseumClient.startChartMuseumServer(s3HelmRepoConfig, awsConfig, "resource-directory", basePath);

    verify(clientHelper, times(1))
        .startS3ChartMuseumServer(bucketName, basePath, region, true, accessKey, secretKey, true);
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

    chartMuseumClient.startChartMuseumServer(helmRepoConfig, gcpConfig, resourceDirectory, basePath);
    verify(clientHelper, times(1))
        .startGCSChartMuseumServer(bucketName, basePath, serviceAccountKey, resourceDirectory);
  }
}
