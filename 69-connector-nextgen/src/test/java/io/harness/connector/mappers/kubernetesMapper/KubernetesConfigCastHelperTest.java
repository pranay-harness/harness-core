package io.harness.connector.mappers.kubernetesMapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.common.kubernetes.KubernetesAuthType;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterDetails;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesDelegateDetails;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.UserNamePasswordK8;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class KubernetesConfigCastHelperTest extends ConnectorsBaseTest {
  @Inject @InjectMocks KubernetesConfigCastHelper kubernetesConfigCastHelper;

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castToKubernetesDelegateCredential() {
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

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castToManualKubernetesCredentials() {
    String masterURL = "masterURL";
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    UserNamePasswordK8 userNamePasswordK8 =
        UserNamePasswordK8.builder().userName(userName).password(password).cacert(cacert).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(userNamePasswordK8)
                                                            .build();
    try {
      KubernetesClusterDetails kubernetesClusterConfig =
          kubernetesConfigCastHelper.castToManualKubernetesCredentials(kubernetesClusterDetails);
    } catch (ClassCastException ex) {
      assertThat(false).isTrue();
    }
  }
}