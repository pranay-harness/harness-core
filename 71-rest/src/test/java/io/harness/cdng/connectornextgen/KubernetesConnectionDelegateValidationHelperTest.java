package io.harness.cdng.connectornextgen;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.CLIENT_KEY_CERT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.SERVICE_ACCOUNT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.fabric8.kubernetes.client.Config;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.k8Connector.ClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.ServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

public class KubernetesConnectionDelegateValidationHelperTest extends WingsBaseTest {
  @Inject @InjectMocks KubernetesValidationHelper kubernetesValidationHelper;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getConfigForUserNamePassword() {
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    String masterUrl = "https://abc.com/";
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(UserNamePasswordDTO.builder().username(userName).password(password).cacert(cacert).build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePassword =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    Config config = kubernetesValidationHelper.getConfig(connectorDTOWithUserNamePassword);
    assertThat(config).isNotNull();
    assertThat(config.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(config.getRequestConfig().getUsername()).isEqualTo(userName);
    assertThat(config.getRequestConfig().getPassword()).isEqualTo(password);
    assertThat(config.getCaCertData()).isEqualTo(cacert);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getConfigForClientKeyCert() {
    String clientKey = "clientKey";
    String clientCert = "clientCert";
    String clientKeyPhrase = "clientKeyPhrase";
    String clientKeyAlgo = "clientKeyAlgo";
    String masterUrl = "https://abc.com/";
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(CLIENT_KEY_CERT)
                                              .credentials(ClientKeyCertDTO.builder()
                                                               .clientKey(clientKey)
                                                               .clientCert(clientCert)
                                                               .clientKeyPassphrase(clientKeyPhrase)
                                                               .clientKeyAlgo(clientKeyAlgo)
                                                               .build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithClientKeyCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    Config config = kubernetesValidationHelper.getConfig(connectorDTOWithClientKeyCreds);
    assertThat(config).isNotNull();
    assertThat(config.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(config.getClientKeyData()).isEqualTo(clientKey);
    assertThat(config.getClientCertData()).isEqualTo(clientCert);
    assertThat(config.getClientKeyPassphrase()).isEqualTo(clientKeyPhrase);
    assertThat(config.getClientKeyAlgo()).isEqualTo(clientKeyAlgo);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getConfigForServiceAccountToken() {
    String serviceAccountKey = "serviceAccountKey";
    String masterUrl = "https://abc.com/";
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(SERVICE_ACCOUNT)
            .credentials(ServiceAccountDTO.builder().serviceAccountToken(serviceAccountKey).build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithServiceAccountCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    Config config = kubernetesValidationHelper.getConfig(connectorDTOWithServiceAccountCreds);
    assertThat(config).isNotNull();
    assertThat(config.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(config.getOauthToken()).isEqualTo(serviceAccountKey);
  }
}