package io.harness.ccm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.config.CEFeatures;
import io.harness.ccm.remote.beans.K8sClusterSetupRequest;
import io.harness.delegate.beans.connector.k8Connector.K8sServiceAccountInfoResponse;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.ImmutableList;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class CEYamlServiceImplTest extends CategoryTest {
  private static final String CONNECTOR_IDENTIFIER = "cId";
  private static final String ACCOUNT_IDENTIFIER = "aId";
  private static final String ORG_IDENTIFIER = "oId";
  private static final String PROJECT_IDENTIFIER = "pId";

  private static final String HARNESS_HOST = "hHost";
  private static final String API_KEY = "aKey";
  private static final String SERVER_NAME = "sName";

  private static final String SERVICE_NAME = "env-admin";
  private static final String SERVICE_NAMESPACE = "env-harness-delegate";

  @Mock private K8sServiceAccountDelegateTaskClient k8sTaskClient;
  @InjectMocks private CEYamlServiceImpl ceYamlService;

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYaml_Visibility_Optimization() throws Exception {
    final K8sServiceAccountInfoResponse response =
        K8sServiceAccountInfoResponse.builder()
            .username("system:serviceaccount:" + SERVICE_NAMESPACE + ":" + SERVICE_NAME)
            .build();

    when(k8sTaskClient.fetchServiceAccount(
             eq(CONNECTOR_IDENTIFIER), eq(ACCOUNT_IDENTIFIER), eq(ORG_IDENTIFIER), eq(PROJECT_IDENTIFIER)))
        .thenReturn(response);

    final K8sClusterSetupRequest request =
        K8sClusterSetupRequest.builder()
            .connectorIdentifier(CONNECTOR_IDENTIFIER)
            .featuresEnabled(ImmutableList.of(CEFeatures.VISIBILITY, CEFeatures.OPTIMIZATION))
            .apiKey(API_KEY)
            .orgIdentifier(ORG_IDENTIFIER)
            .projectIdentifier(PROJECT_IDENTIFIER)
            .build();

    final String yamlContent =
        ceYamlService.unifiedCloudCostK8sClusterYaml(ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request);

    assertThat(yamlContent).isNotNull();

    assertContainsOptimizationParams(yamlContent);

    assertContainsVisibilityParams(yamlContent);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYamlFileStructure() throws Exception {
    final K8sServiceAccountInfoResponse response =
        K8sServiceAccountInfoResponse.builder()
            .username("system:serviceaccount:" + SERVICE_NAMESPACE + ":" + SERVICE_NAME)
            .build();

    when(k8sTaskClient.fetchServiceAccount(
             eq(CONNECTOR_IDENTIFIER), eq(ACCOUNT_IDENTIFIER), eq(ORG_IDENTIFIER), eq(PROJECT_IDENTIFIER)))
        .thenReturn(response);

    final K8sClusterSetupRequest request =
        K8sClusterSetupRequest.builder()
            .connectorIdentifier(CONNECTOR_IDENTIFIER)
            .featuresEnabled(ImmutableList.of(CEFeatures.VISIBILITY, CEFeatures.OPTIMIZATION))
            .apiKey(API_KEY)
            .orgIdentifier(ORG_IDENTIFIER)
            .projectIdentifier(PROJECT_IDENTIFIER)
            .build();

    final String actualYamlContent =
        ceYamlService.unifiedCloudCostK8sClusterYaml(ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request);

    final String expectedYamlContent = IOUtils.toString(
        this.getClass().getResourceAsStream("/yaml/unified-k8s-cluster-setup.yaml"), StandardCharsets.UTF_8);

    assertThat(actualYamlContent).isEqualTo(expectedYamlContent);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYaml_Visibility() throws Exception {
    final K8sServiceAccountInfoResponse response =
        K8sServiceAccountInfoResponse.builder()
            .username("system:serviceaccount:" + SERVICE_NAMESPACE + ":" + SERVICE_NAME)
            .build();

    when(k8sTaskClient.fetchServiceAccount(
             eq(CONNECTOR_IDENTIFIER), eq(ACCOUNT_IDENTIFIER), eq(ORG_IDENTIFIER), eq(PROJECT_IDENTIFIER)))
        .thenReturn(response);

    final K8sClusterSetupRequest request = K8sClusterSetupRequest.builder()
                                               .connectorIdentifier(CONNECTOR_IDENTIFIER)
                                               .featuresEnabled(ImmutableList.of(CEFeatures.VISIBILITY))
                                               .orgIdentifier(ORG_IDENTIFIER)
                                               .projectIdentifier(PROJECT_IDENTIFIER)
                                               .build();

    final String yamlContent =
        ceYamlService.unifiedCloudCostK8sClusterYaml(ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request);

    assertThat(yamlContent).isNotNull();

    assertContainsVisibilityParams(yamlContent);

    // OPTIMIZATION Check
    assertThat(yamlContent).doesNotContain(CONNECTOR_IDENTIFIER);
    assertThat(yamlContent).doesNotContain(ACCOUNT_IDENTIFIER);
    assertThat(yamlContent).doesNotContain(HARNESS_HOST);
    assertThat(yamlContent).doesNotContain(API_KEY);
    assertThat(yamlContent).doesNotContain(SERVER_NAME);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYaml_Optimization() throws Exception {
    final K8sClusterSetupRequest request = K8sClusterSetupRequest.builder()
                                               .connectorIdentifier(CONNECTOR_IDENTIFIER)
                                               .featuresEnabled(ImmutableList.of(CEFeatures.OPTIMIZATION))
                                               .apiKey(API_KEY)
                                               .orgIdentifier(ORG_IDENTIFIER)
                                               .projectIdentifier(PROJECT_IDENTIFIER)
                                               .build();

    final String yamlContent =
        ceYamlService.unifiedCloudCostK8sClusterYaml(ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request);

    assertThat(yamlContent).isNotNull();

    // VISIBILITY Check
    assertThat(yamlContent).doesNotContain(SERVICE_NAME);
    assertThat(yamlContent).doesNotContain(SERVICE_NAMESPACE);

    assertContainsOptimizationParams(yamlContent);

    verifyZeroInteractions(k8sTaskClient);
  }

  private void assertContainsVisibilityParams(String yamlContent) {
    assertThat(yamlContent).contains(SERVICE_NAME);
    assertThat(yamlContent).contains(SERVICE_NAMESPACE);
  }

  private void assertContainsOptimizationParams(String yamlContent) {
    assertThat(yamlContent).contains(CONNECTOR_IDENTIFIER);
    assertThat(yamlContent).contains(ACCOUNT_IDENTIFIER);
    assertThat(yamlContent).contains(HARNESS_HOST);
    assertThat(yamlContent).contains(API_KEY);
    assertThat(yamlContent).contains(SERVER_NAME);
  }
}