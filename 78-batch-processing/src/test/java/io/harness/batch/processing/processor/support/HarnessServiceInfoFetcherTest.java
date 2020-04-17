package io.harness.batch.processing.processor.support;

import static io.harness.batch.processing.writer.constants.K8sCCMConstants.K8SV1_RELEASE_NAME;
import static io.harness.batch.processing.writer.constants.K8sCCMConstants.RELEASE_NAME;
import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.Optional;

public class HarnessServiceInfoFetcherTest extends CategoryTest {
  private static final String POD_NAME = "podName";
  private static final String NAMESPACE = "namespace";
  private static final String SETTING_ID = "settingId";
  private static final String ACCOUNT_ID = "fcf53242-4a9d-4b8c-8497-5ba7360569d9";
  private HarnessServiceInfoFetcher harnessServiceInfoFetcher;
  private K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher;
  private CloudToHarnessMappingService cloudToHarnessMappingService;

  @Before
  public void setUp() throws Exception {
    cloudToHarnessMappingService = mock(CloudToHarnessMappingService.class);
    k8sLabelServiceInfoFetcher = mock(K8sLabelServiceInfoFetcher.class);
    harnessServiceInfoFetcher = new HarnessServiceInfoFetcher(k8sLabelServiceInfoFetcher, cloudToHarnessMappingService);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnHarnessSvcInfoWhenK8sV1LabelPresentAndMappingFound() throws Exception {
    ImmutableMap<String, String> labels = ImmutableMap.of(K8SV1_RELEASE_NAME, "value1");
    when(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, labels)).thenReturn(Optional.ofNullable(null));
    HarnessServiceInfo harnessServiceInfo = new HarnessServiceInfo(
        "svc-id", "app-id", "cloud-provider-id", "env-id", "infra-mapping-id", "deployment-summary-id");
    when(cloudToHarnessMappingService.getHarnessServiceInfo(ACCOUNT_ID, SETTING_ID, NAMESPACE, POD_NAME))
        .thenReturn(Optional.of(harnessServiceInfo));
    assertThat(harnessServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, SETTING_ID, NAMESPACE, POD_NAME, labels))
        .isPresent()
        .hasValue(harnessServiceInfo);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnHarnessSvcInfoWhenLabelPresentAndMappingFound() throws Exception {
    ImmutableMap<String, String> labels = ImmutableMap.of(RELEASE_NAME, "value1");
    HarnessServiceInfo harnessServiceInfo = new HarnessServiceInfo(
        "svc-id", "app-id", "cloud-provider-id", "env-id", "infra-mapping-id", "deployment-summary-id");
    when(cloudToHarnessMappingService.getHarnessServiceInfo(ACCOUNT_ID, SETTING_ID, NAMESPACE, POD_NAME))
        .thenReturn(Optional.empty());
    when(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, labels))
        .thenReturn(Optional.of(harnessServiceInfo));
    assertThat(harnessServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, SETTING_ID, NAMESPACE, POD_NAME, labels))
        .isPresent()
        .hasValue(harnessServiceInfo);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnEmptyOptionalWhenNoReleaseNameLabel() throws Exception {
    ImmutableMap<String, String> labels = ImmutableMap.of("key1", "value1");

    when(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, labels)).thenThrow(Exception.class);
    assertThat(harnessServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, SETTING_ID, NAMESPACE, POD_NAME, labels))
        .isNotPresent();
  }
}
