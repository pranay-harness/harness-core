package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.encryption.SecretRefData.SECRET_DELIMINITER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.k8connector.KubernetesConfigSummaryDTO;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class KubernetesConfigSummaryMapperTest extends CategoryTest {
  @InjectMocks KubernetesConfigSummaryMapper kubernetesConfigSummaryMapper;
  @Mock KubernetesConfigCastHelper kubernetesConfigCastHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(kubernetesConfigCastHelper.castToKubernetesDelegateCredential(any())).thenCallRealMethod();
    when(kubernetesConfigCastHelper.castToManualKubernetesCredentials(any())).thenCallRealMethod();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void createKubernetesConfigSummaryDTOForDelegateCredentials() {
    String delegateName = "testDeleagete";
    KubernetesDelegateDetails delegateCredential =
        KubernetesDelegateDetails.builder().delegateName(delegateName).build();
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().credentialType(INHERIT_FROM_DELEGATE).credential(delegateCredential).build();
    KubernetesConfigSummaryDTO kubernetesConfigSummaryDTO =
        kubernetesConfigSummaryMapper.toConnectorConfigSummaryDTO(kubernetesClusterConfig);
    assertThat(kubernetesConfigSummaryDTO).isNotNull();
    assertThat(kubernetesConfigSummaryDTO.getDelegateName()).isEqualTo(delegateName);
    assertThat(kubernetesConfigSummaryDTO.getMasterURL()).isBlank();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void createKubernetesConfigSummaryDTOForManualCredentials() {
    String masterURL = "masterURL";
    String userName = "userName";
    String cacert = "caCertRef";
    String passwordIdentifier = "passwordIdentifier";
    String cacertIdentifier = "cacertIdentifier";
    SecretRefData secretRefDataCACert = SecretRefData.builder().identifier(cacert).scope(Scope.ACCOUNT).build();
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build();
    String passwordRef = "acc" + SECRET_DELIMINITER + passwordIdentifier;
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).passwordRef(passwordRef).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder()
                                                          .credentialType(MANUAL_CREDENTIALS)
                                                          .credential(kubernetesClusterDetails)
                                                          .build();

    KubernetesConfigSummaryDTO kubernetesConfigSummaryDTO =
        kubernetesConfigSummaryMapper.toConnectorConfigSummaryDTO(kubernetesClusterConfig);
    assertThat(kubernetesConfigSummaryDTO).isNotNull();
    assertThat(kubernetesConfigSummaryDTO.getMasterURL()).isEqualTo(masterURL);
    assertThat(kubernetesConfigSummaryDTO.getDelegateName()).isBlank();
  }
}