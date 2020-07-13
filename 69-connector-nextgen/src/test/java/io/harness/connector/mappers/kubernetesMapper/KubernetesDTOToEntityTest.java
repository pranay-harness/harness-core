package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.CLIENT_KEY_CERT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.OPEN_ID_CONNECT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.SERVICE_ACCOUNT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.USER_PASSWORD;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.ClientKeyCertK8;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.connector.entities.embedded.kubernetescluster.OpenIdConnectK8;
import io.harness.connector.entities.embedded.kubernetescluster.ServiceAccountK8;
import io.harness.connector.entities.embedded.kubernetescluster.UserNamePasswordK8;
import io.harness.delegate.beans.connector.k8Connector.ClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.OpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.ServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class KubernetesDTOToEntityTest extends CategoryTest {
  @InjectMocks KubernetesDTOToEntity kubernetesDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForDelegateCredentials() {
    String delegateName = "testDeleagete";
    KubernetesClusterConfigDTO connectorDTOWithDelegateCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
            .config(KubernetesDelegateDetailsDTO.builder().delegateName(delegateName).build())
            .build();
    Connector connector = kubernetesDTOToEntity.toKubernetesClusterConfig(connectorDTOWithDelegateCreds);
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(INHERIT_FROM_DELEGATE);
    KubernetesDelegateDetails kubernetesCredential = (KubernetesDelegateDetails) k8Config.getCredential();
    assertThat(kubernetesCredential.getDelegateName()).isEqualTo(delegateName);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForUserNamePasswordCredential() {
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    String masterUrl = "https://abc.com";
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(UserNamePasswordDTO.builder().username(userName).password(password).cacert(cacert).build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePasswordCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    Connector connector = kubernetesDTOToEntity.toKubernetesClusterConfig(connectorDTOWithUserNamePasswordCreds);
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetails kubernetesClusterDetails = (KubernetesClusterDetails) k8Config.getCredential();
    assertThat(kubernetesClusterDetails.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(kubernetesClusterDetails.getAuthType()).isEqualTo(USER_PASSWORD);
    UserNamePasswordK8 kubernetesCredential = (UserNamePasswordK8) kubernetesClusterDetails.getAuth();
    assertThat(kubernetesCredential.getUserName()).isEqualTo(userName);
    assertThat(kubernetesCredential.getPassword()).isEqualTo(password);
    assertThat(kubernetesCredential.getCacert()).isEqualTo(cacert);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForClientKeyCert() {
    String clientKey = "clientKey";
    String clientCert = "clientCert";
    String clientKeyPhrase = "clientKeyPhrase";
    String clientKeyAlgo = "clientKeyAlgo";
    String masterUrl = "https://abc.com";
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
    Connector connector = kubernetesDTOToEntity.toKubernetesClusterConfig(connectorDTOWithClientKeyCreds);
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetails kubernetesClusterDetails = (KubernetesClusterDetails) k8Config.getCredential();
    assertThat(kubernetesClusterDetails.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(kubernetesClusterDetails.getAuthType()).isEqualTo(CLIENT_KEY_CERT);
    ClientKeyCertK8 kubernetesCredential = (ClientKeyCertK8) kubernetesClusterDetails.getAuth();
    assertThat(kubernetesCredential.getClientKey()).isEqualTo(clientKey);
    assertThat(kubernetesCredential.getClientCert()).isEqualTo(clientCert);
    assertThat(kubernetesCredential.getClientKeyPassphrase()).isEqualTo(clientKeyPhrase);
    assertThat(kubernetesCredential.getClientKeyAlgo()).isEqualTo(clientKeyAlgo);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForOIDCConnect() {
    String oidClientId = "oidcClientId";
    String oidcIssuerUrl = "oidcIssuerUrl";
    String oidcPassword = "oidcPassword";
    String oidcScopes = "oidcScopes";
    String oidcSecret = "oidcSecret";
    String oidcUsername = "oidcUsername";
    String masterUrl = "https://abc.com";
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(OPEN_ID_CONNECT)
                                              .credentials(OpenIdConnectDTO.builder()
                                                               .oidcClientId(oidClientId)
                                                               .oidcIssuerUrl(oidcIssuerUrl)
                                                               .oidcPassword(oidcPassword)
                                                               .oidcScopes(oidcScopes)
                                                               .oidcSecret(oidcSecret)
                                                               .oidcUsername(oidcUsername)
                                                               .build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithOpenIdConnectCred =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    Connector connector = kubernetesDTOToEntity.toKubernetesClusterConfig(connectorDTOWithOpenIdConnectCred);
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetails kubernetesClusterDetails = (KubernetesClusterDetails) k8Config.getCredential();
    assertThat(kubernetesClusterDetails.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(kubernetesClusterDetails.getAuthType()).isEqualTo(OPEN_ID_CONNECT);
    OpenIdConnectK8 kubernetesCredential = (OpenIdConnectK8) kubernetesClusterDetails.getAuth();
    assertThat(kubernetesCredential.getOidcClientId()).isEqualTo(oidClientId);
    assertThat(kubernetesCredential.getOidcIssuerUrl()).isEqualTo(oidcIssuerUrl);
    assertThat(kubernetesCredential.getOidcPassword()).isEqualTo(oidcPassword);
    assertThat(kubernetesCredential.getOidcScopes()).isEqualTo(oidcScopes);
    assertThat(kubernetesCredential.getOidcSecret()).isEqualTo(oidcSecret);
    assertThat(kubernetesCredential.getOidcUsername()).isEqualTo(oidcUsername);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForServiceAccount() {
    String serviceAccountKey = "serviceAccountKey";
    String masterUrl = "https://abc.com";
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
    Connector connector = kubernetesDTOToEntity.toKubernetesClusterConfig(connectorDTOWithServiceAccountCreds);
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetails kubernetesClusterDetails = (KubernetesClusterDetails) k8Config.getCredential();
    assertThat(kubernetesClusterDetails.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(kubernetesClusterDetails.getAuthType()).isEqualTo(SERVICE_ACCOUNT);
    ServiceAccountK8 kubernetesCredential = (ServiceAccountK8) kubernetesClusterDetails.getAuth();
    assertThat(kubernetesCredential.getServiceAcccountToken()).isEqualTo(serviceAccountKey);
  }
}