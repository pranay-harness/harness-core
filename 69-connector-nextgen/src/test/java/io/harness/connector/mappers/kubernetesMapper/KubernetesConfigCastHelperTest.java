package io.harness.connector.mappers.kubernetesMapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.exception.UnexpectedException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class KubernetesConfigCastHelperTest extends CategoryTest {
  @InjectMocks KubernetesConfigCastHelper kubernetesConfigCastHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castToKubernetesDelegateCredentialTest() {
    String delegateName = "testDeleagete";
    KubernetesDelegateDetails delegateCredential =
        KubernetesDelegateDetails.builder().delegateName(delegateName).build();
    try {
      KubernetesDelegateDetails delegateDetails =
          kubernetesConfigCastHelper.castToKubernetesDelegateCredential(delegateCredential);
    } catch (ClassCastException ex) {
      assertThat(false).isTrue();
    }
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castManualCredentialToKubernetesDelegateCredentialTest() {
    String delegateName = "testDeleagete";
    String masterURL = "masterURL";
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).password(password).cacert(cacert).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    KubernetesDelegateDetails delegateDetails =
        kubernetesConfigCastHelper.castToKubernetesDelegateCredential(kubernetesClusterDetails);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castToManualKubernetesCredentialsTest() {
    String masterURL = "masterURL";
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).password(password).cacert(cacert).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    try {
      KubernetesClusterDetails kubernetesClusterConfig =
          kubernetesConfigCastHelper.castToManualKubernetesCredentials(kubernetesClusterDetails);
    } catch (ClassCastException ex) {
      assertThat(false).isTrue();
    }
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castDelegateCredsToManualKubernetesCredentialsTest() {
    String delegateName = "testDeleagete";
    KubernetesDelegateDetails delegateCredential =
        KubernetesDelegateDetails.builder().delegateName(delegateName).build();
    KubernetesClusterDetails kubernetesClusterConfig =
        kubernetesConfigCastHelper.castToManualKubernetesCredentials(delegateCredential);
  }
}