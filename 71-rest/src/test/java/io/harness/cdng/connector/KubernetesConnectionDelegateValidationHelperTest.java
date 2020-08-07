package io.harness.cdng.connector;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.CLIENT_KEY_CERT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.SERVICE_ACCOUNT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.client.Config;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class KubernetesConnectionDelegateValidationHelperTest extends CategoryTest {
  @InjectMocks KubernetesValidationHelper kubernetesValidationHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getConfigForUserNamePassword() {
    String userName = "userName";
    String password = "password";
    String masterUrl = "https://abc.com/";
    String passwordIdentifier = "passwordIdentifer";
    String passwordRef = "acc:" + passwordIdentifier;
    String cacert = "caCertRef";
    SecretRefData passwordSecretRefData =
        SecretRefData.builder().identifier(cacert).scope(Scope.ACCOUNT).decryptedValue(password.toCharArray()).build();
    SecretRefData secretRefDataCACert = SecretRefData.builder()
                                            .identifier(passwordIdentifier)
                                            .scope(Scope.ACCOUNT)
                                            .decryptedValue(cacert.toCharArray())
                                            .build();
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(KubernetesAuthType.USER_PASSWORD)
                                              .credentials(KubernetesUserNamePasswordDTO.builder()
                                                               .username(userName)
                                                               .passwordRef(passwordSecretRefData)
                                                               .caCertRef(secretRefDataCACert)
                                                               .build())
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
    String clientKeyIdentifier = "clientKeyRef";
    String clientCert = "clientCert";
    String clientCertIdentifier = "clientCertRef";
    String clientKeyPhrase = "clientKeyPhrase";
    String clientKeyPhraseIdentifier = "clientKeyPhraseIdentifier";
    String clientKeyAlgo = "clientKeyAlgo";
    String masterUrl = "https://abc.com/";
    SecretRefData clientKeySecret = SecretRefData.builder()
                                        .identifier(clientKeyIdentifier)
                                        .scope(Scope.ACCOUNT)
                                        .decryptedValue(clientKey.toCharArray())
                                        .build();
    SecretRefData clientCertSecret = SecretRefData.builder()
                                         .identifier(clientCertIdentifier)
                                         .scope(Scope.ACCOUNT)
                                         .decryptedValue(clientCert.toCharArray())
                                         .build();
    SecretRefData clientKeyPassPhraseSecret = SecretRefData.builder()
                                                  .identifier(clientKeyPhraseIdentifier)
                                                  .scope(Scope.ACCOUNT)
                                                  .decryptedValue(clientKeyPhrase.toCharArray())
                                                  .build();
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(CLIENT_KEY_CERT)
                                              .credentials(KubernetesClientKeyCertDTO.builder()
                                                               .clientKeyRef(clientKeySecret)
                                                               .clientCertRef(clientCertSecret)
                                                               .clientKeyPassphraseRef(clientKeyPassPhraseSecret)
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
    String serviceAccountRef = "serviceAccountRef";
    SecretRefData passwordSecretRef = SecretRefData.builder()
                                          .identifier(serviceAccountRef)
                                          .scope(Scope.ACCOUNT)
                                          .decryptedValue(serviceAccountKey.toCharArray())
                                          .build();
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(SERVICE_ACCOUNT)
            .credentials(KubernetesServiceAccountDTO.builder().serviceAccountTokenRef(passwordSecretRef).build())
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