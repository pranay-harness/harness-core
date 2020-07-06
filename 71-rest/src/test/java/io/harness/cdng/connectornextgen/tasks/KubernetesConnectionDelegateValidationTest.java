package io.harness.cdng.connectornextgen.tasks;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.network.Http;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Http.class})
public class KubernetesConnectionDelegateValidationTest {
  private String delegateName = "delegateName";
  private String masterUrl = "masterUrl";

  @Before
  public void setup() {
    initMocks(this);
    PowerMockito.mockStatic(Http.class);
  }

  @InjectMocks
  private KubernetesConnectionDelegateValidation kubernetesConnectionDelegateValidationTask =
      new KubernetesConnectionDelegateValidation(generateUuid(),
          DelegateTask.builder()
              .data(
                  (TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                      .parameters(new Object[] {
                          KubernetesConnectionTaskParams.builder()
                              .kubernetesClusterConfig(
                                  KubernetesClusterConfigDTO.builder()
                                      .kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE)
                                      .config(KubernetesDelegateDetailsDTO.builder().delegateName(delegateName).build())
                                      .build())
                              .build()})
                      .build())
              .build(),
          null);

  @InjectMocks
  private KubernetesConnectionDelegateValidation kubernetesConnectionDelegateValidationTaskManualCreds =
      new KubernetesConnectionDelegateValidation(generateUuid(),
          DelegateTask.builder()
              .data((TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                        .parameters(new Object[] {
                            KubernetesConnectionTaskParams.builder()
                                .kubernetesClusterConfig(
                                    KubernetesClusterConfigDTO.builder()
                                        .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                                        .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).build())
                                        .build())
                                .build()})
                        .build())
              .build(),
          null);

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void validateForInclusterDelegate() {
    PowerMockito.when(Http.connectableHttpUrl(any())).thenReturn(true);
    List<DelegateConnectionResult> delegateConnectionResults = kubernetesConnectionDelegateValidationTask.validate();
    assertThat(delegateConnectionResults.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void validateWhenUserNamePasswordGiven() {
    PowerMockito.when(Http.connectableHttpUrl(any())).thenReturn(true);
    List<DelegateConnectionResult> delegateConnectionResults =
        kubernetesConnectionDelegateValidationTaskManualCreds.validate();
    assertThat(delegateConnectionResults.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getCriteriaForInclusterDelegate() {
    List<String> criteria = kubernetesConnectionDelegateValidationTask.getCriteria();
    assertThat(criteria.get(0)).isEqualTo(delegateName);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getCriteriaForManualCredential() {
    List<String> criteria = kubernetesConnectionDelegateValidationTaskManualCreds.getCriteria();
    assertThat(criteria.get(0)).isEqualTo(masterUrl);
  }
}