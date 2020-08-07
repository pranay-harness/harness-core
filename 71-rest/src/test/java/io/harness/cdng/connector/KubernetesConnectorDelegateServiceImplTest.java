package io.harness.cdng.connector;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.connector.impl.KubernetesConnectorDelegateServiceImpl;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

public class KubernetesConnectorDelegateServiceImplTest extends WingsBaseTest {
  @Mock KubernetesValidationHelper kubernetesValidationHelper;
  @Inject @InjectMocks KubernetesConnectorDelegateServiceImpl kubernetesConnectorServiceImpl;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void validate() {
    String userName = "userName";
    String password = "password";
    String masterUrl = "https://abc.com";
    String identifier = "identifier";
    String name = "name";
    String passwordIdentifier = "passwordIdentifer";
    String passwordRef = "acc:" + passwordIdentifier;
    String cacert = "caCertRef";
    SecretRefData secretRefDataCACert = SecretRefData.builder().identifier(cacert).scope(Scope.ACCOUNT).build();
    SecretRefData passwordSecretRefData =
        SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build();
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(KubernetesAuthType.USER_PASSWORD)
                                              .credentials(KubernetesUserNamePasswordDTO.builder()
                                                               .username(userName)
                                                               .passwordRef(passwordSecretRefData)
                                                               .caCertRef(secretRefDataCACert)
                                                               .build())
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