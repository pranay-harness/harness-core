package io.harness.ccm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.junit.Before;
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
  private static final String CCM_CONNECTOR_IDENTIFIER = "VALUE_REPLACED_BY_UNIT_TEST_ccmId";
  private static final String ACCOUNT_IDENTIFIER = "VALUE_REPLACED_BY_UNIT_TEST_aId";
  private static final String ORG_IDENTIFIER = "VALUE_REPLACED_BY_UNIT_TEST_oId";
  private static final String PROJECT_IDENTIFIER = "VALUE_REPLACED_BY_UNIT_TEST_pId";

  private static final String HARNESS_HOST = "VALUE_REPLACED_BY_UNIT_TEST_hHost";
  private static final String SERVER_NAME = "VALUE_REPLACED_BY_UNIT_TEST_sName";

  private static final String SERVICE_NAME = "VALUE_REPLACED_BY_UNIT_TEST_env-admin";
  private static final String SERVICE_NAMESPACE = "VALUE_REPLACED_BY_UNIT_TEST_env-harness-delegate";

  @Mock private K8sServiceAccountDelegateTaskClient k8sServiceAccountDelegateTaskClient;
  @InjectMocks private CEYamlServiceImpl ceYamlService;

  @Before
  public void setup() {
    final K8sServiceAccountInfoResponse response =
        K8sServiceAccountInfoResponse.builder()
            .username("system:serviceaccount:" + SERVICE_NAMESPACE + ":" + SERVICE_NAME)
            .build();

    when(k8sServiceAccountDelegateTaskClient.fetchServiceAccount(
             eq(CONNECTOR_IDENTIFIER), eq(ACCOUNT_IDENTIFIER), eq(ORG_IDENTIFIER), eq(PROJECT_IDENTIFIER)))
        .thenReturn(response);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYaml_Visibility_Optimization() throws Exception {
    final K8sClusterSetupRequest request =
        K8sClusterSetupRequest.builder()
            .connectorIdentifier(CONNECTOR_IDENTIFIER)
            .featuresEnabled(ImmutableList.of(CEFeatures.VISIBILITY, CEFeatures.OPTIMIZATION))
            .orgIdentifier(ORG_IDENTIFIER)
            .projectIdentifier(PROJECT_IDENTIFIER)
            .ccmConnectorIdentifier(CCM_CONNECTOR_IDENTIFIER)
            .build();

    final String yamlContent = ceYamlService.unifiedCloudCostK8sClusterYaml(
        ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request, true, true);

    verify(k8sServiceAccountDelegateTaskClient, times(1)).fetchServiceAccount(any(), any(), any(), any());

    assertThat(yamlContent).isNotBlank();

    assertContainsVisibilityParams(yamlContent);
    assertContainsOptimizationParams(yamlContent);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYamlFileStructure() throws Exception {
    final K8sClusterSetupRequest request =
        K8sClusterSetupRequest.builder()
            .connectorIdentifier(CONNECTOR_IDENTIFIER)
            .featuresEnabled(ImmutableList.of(CEFeatures.VISIBILITY, CEFeatures.OPTIMIZATION))
            .orgIdentifier(ORG_IDENTIFIER)
            .projectIdentifier(PROJECT_IDENTIFIER)
            .ccmConnectorIdentifier(CCM_CONNECTOR_IDENTIFIER)
            .build();

    final String actualYamlContent = ceYamlService.unifiedCloudCostK8sClusterYaml(
        ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request, true, true);

    final String expectedYamlContent = IOUtils.toString(
        this.getClass().getResourceAsStream("/yaml/unified-k8s-cluster-setup.yaml"), StandardCharsets.UTF_8);

    verify(k8sServiceAccountDelegateTaskClient, times(1)).fetchServiceAccount(any(), any(), any(), any());

    assertThat(actualYamlContent).isNotBlank();
    assertThat(actualYamlContent).isEqualTo(expectedYamlContent);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYaml_Visibility() throws Exception {
    final K8sClusterSetupRequest request = K8sClusterSetupRequest.builder()
                                               .connectorIdentifier(CONNECTOR_IDENTIFIER)
                                               .featuresEnabled(ImmutableList.of(CEFeatures.VISIBILITY))
                                               .orgIdentifier(ORG_IDENTIFIER)
                                               .projectIdentifier(PROJECT_IDENTIFIER)
                                               .build();

    final String yamlContent = ceYamlService.unifiedCloudCostK8sClusterYaml(
        ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request, true, false);

    verify(k8sServiceAccountDelegateTaskClient, times(1)).fetchServiceAccount(any(), any(), any(), any());

    assertThat(yamlContent).isNotBlank();

    assertContainsVisibilityParams(yamlContent);

    // since optimization is not asked
    assertThat(yamlContent).doesNotContain(CCM_CONNECTOR_IDENTIFIER);
    assertThat(yamlContent).doesNotContain(ACCOUNT_IDENTIFIER);
    assertThat(yamlContent).doesNotContain(HARNESS_HOST);
    assertThat(yamlContent).doesNotContain(SERVER_NAME);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYaml_Optimization() throws Exception {
    final K8sClusterSetupRequest request = K8sClusterSetupRequest.builder()
                                               .featuresEnabled(ImmutableList.of(CEFeatures.OPTIMIZATION))
                                               .ccmConnectorIdentifier(CCM_CONNECTOR_IDENTIFIER)
                                               .orgIdentifier(ORG_IDENTIFIER)
                                               .projectIdentifier(PROJECT_IDENTIFIER)
                                               .connectorIdentifier(CONNECTOR_IDENTIFIER)
                                               .build();

    final String yamlContent = ceYamlService.unifiedCloudCostK8sClusterYaml(
        ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request, false, true);

    verifyZeroInteractions(k8sServiceAccountDelegateTaskClient);

    assertThat(yamlContent).isNotBlank();

    assertContainsOptimizationParams(yamlContent);

    // since visibility is not asked
    assertThat(yamlContent).doesNotContain(SERVICE_NAME);
    assertThat(yamlContent).doesNotContain(SERVICE_NAMESPACE);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUnifiedCloudCostK8sClusterYaml_Visibility_DelegateTaskError() throws Exception {
    final K8sClusterSetupRequest request = K8sClusterSetupRequest.builder()
                                               .connectorIdentifier(CONNECTOR_IDENTIFIER)
                                               .featuresEnabled(ImmutableList.of(CEFeatures.VISIBILITY))
                                               .orgIdentifier(ORG_IDENTIFIER)
                                               .projectIdentifier(PROJECT_IDENTIFIER)
                                               .build();

    when(k8sServiceAccountDelegateTaskClient.fetchServiceAccount(any(), any(), any(), any()))
        .thenThrow(new RuntimeException(""));

    final String yamlContent = ceYamlService.unifiedCloudCostK8sClusterYaml(
        ACCOUNT_IDENTIFIER, HARNESS_HOST, SERVER_NAME, request, true, false);

    assertThat(yamlContent).isNotBlank();

    // assert default values for ServiceAccount
    assertThat(yamlContent).contains("name: <REPLACE_WITH_SERVICEACCOUNT_NAME_USED_BY_DELEGATE>");
    assertThat(yamlContent).contains("namespace: <REPLACE_WITH_NAMESPACE_IN_WHICH_THE_DELEGATE_IS_INSTALLED>");

    // since optimization is not asked
    assertThat(yamlContent).doesNotContain(CCM_CONNECTOR_IDENTIFIER);
    assertThat(yamlContent).doesNotContain(ACCOUNT_IDENTIFIER);
    assertThat(yamlContent).doesNotContain(HARNESS_HOST);
    assertThat(yamlContent).doesNotContain(SERVER_NAME);
  }

  private void assertContainsVisibilityParams(String yamlContent) {
    assertThat(yamlContent).contains(SERVICE_NAME);
    assertThat(yamlContent).contains(SERVICE_NAMESPACE);
  }

  private void assertContainsOptimizationParams(String yamlContent) {
    assertThat(yamlContent).contains(CCM_CONNECTOR_IDENTIFIER);

    assertThat(yamlContent).contains(ACCOUNT_IDENTIFIER);
    assertThat(yamlContent).contains(HARNESS_HOST);
  }
}