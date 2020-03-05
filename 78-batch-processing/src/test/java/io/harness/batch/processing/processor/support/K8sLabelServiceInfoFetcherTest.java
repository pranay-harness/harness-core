package io.harness.batch.processing.processor.support;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import software.wings.api.DeploymentSummary;
import software.wings.api.K8sDeploymentInfo;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.Optional;

public class K8sLabelServiceInfoFetcherTest extends CategoryTest {
  private static final String ACCOUNT_ID = "fcf53242-4a9d-4b8c-8497-5ba7360569d9";
  private K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher;
  private CloudToHarnessMappingService cloudToHarnessMappingService;

  @Before
  public void setUp() throws Exception {
    cloudToHarnessMappingService = mock(CloudToHarnessMappingService.class);
    k8sLabelServiceInfoFetcher = new K8sLabelServiceInfoFetcher(cloudToHarnessMappingService);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldReturnEmptyOptionalWhenNoReleaseNameLabel() throws Exception {
    assertThat(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, ImmutableMap.of("key1", "value1")))
        .isNotPresent();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldReturnEmptyOptionalWhenCloudToHarnessMappingNotFound() throws Exception {
    String relName = "115856c0-8bd5-4d5c-901b-a9abe538f6c4";
    ArgumentCaptor<DeploymentSummary> captor = ArgumentCaptor.forClass(DeploymentSummary.class);
    when(cloudToHarnessMappingService.getHarnessServiceInfo(captor.capture())).thenReturn(Optional.empty());
    assertThat(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(
                   ACCOUNT_ID, ImmutableMap.of("key1", "value1", K8sCCMConstants.RELEASE_NAME, relName)))
        .isNotPresent();
    assertThat(captor.getValue()).satisfies(deploymentSummary -> {
      assertThat(deploymentSummary.getAccountId()).isEqualTo(ACCOUNT_ID);
      assertThat(deploymentSummary.getK8sDeploymentKey().getReleaseName()).isEqualTo(relName);
      assertThat(deploymentSummary.getDeploymentInfo())
          .isInstanceOfSatisfying(K8sDeploymentInfo.class,
              k8sDeploymentInfo -> { assertThat(k8sDeploymentInfo.getReleaseName()).isEqualTo(relName); });
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldReturnHarnessSvcInfoWhenLabelPresentAndMappingFound() throws Exception {
    String relName = "115856c0-8bd5-4d5c-901b-a9abe538f6c4";
    ArgumentCaptor<DeploymentSummary> captor = ArgumentCaptor.forClass(DeploymentSummary.class);
    HarnessServiceInfo harnessServiceInfo = new HarnessServiceInfo(
        "svc-id", "app-id", "cloud-provider-id", "env-id", "infra-mapping-id", "deployment-summary-id");
    when(cloudToHarnessMappingService.getHarnessServiceInfo(captor.capture()))
        .thenReturn(Optional.of(harnessServiceInfo));
    assertThat(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(
                   ACCOUNT_ID, ImmutableMap.of("key1", "value1", K8sCCMConstants.RELEASE_NAME, relName)))
        .isPresent()
        .hasValue(harnessServiceInfo);
    assertThat(captor.getValue()).satisfies(deploymentSummary -> {
      assertThat(deploymentSummary.getAccountId()).isEqualTo(ACCOUNT_ID);
      assertThat(deploymentSummary.getK8sDeploymentKey().getReleaseName()).isEqualTo(relName);
      assertThat(deploymentSummary.getDeploymentInfo())
          .isInstanceOfSatisfying(K8sDeploymentInfo.class,
              k8sDeploymentInfo -> { assertThat(k8sDeploymentInfo.getReleaseName()).isEqualTo(relName); });
    });
  }
}
