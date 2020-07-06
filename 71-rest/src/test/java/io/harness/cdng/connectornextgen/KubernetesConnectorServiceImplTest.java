package io.harness.cdng.connectornextgen;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.connectornextgen.impl.KubernetesConnectorServiceImpl;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

public class KubernetesConnectorServiceImplTest extends WingsBaseTest {
  @Mock KubernetesValidationHelper kubernetesValidationHelper;
  @Inject @InjectMocks KubernetesConnectorServiceImpl kubernetesConnectorServiceImpl;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void validate() {
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    String masterUrl = "https://abc.com";
    String identifier = "identifier";
    String name = "name";
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(UserNamePasswordDTO.builder().username(userName).password(password).cacert(cacert).build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithUsernameCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    kubernetesConnectorServiceImpl.validate(connectorDTOWithUsernameCreds);
    verify(kubernetesValidationHelper, times(1)).listControllers(any());
  }
}