package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.K8sClientKeyCert;
import io.harness.connector.entities.embedded.kubernetescluster.K8sOpenIdConnect;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class KubernetesEntityToDTOTest extends CategoryTest {
  @InjectMocks KubernetesEntityToDTO kubernetesEntityToDTO;
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
  public void testCreateK8ClusterConfigDTOForDelegateCredentials() {
    String delegateName = "testDeleagete";
    KubernetesDelegateDetails delegateCredential =
        KubernetesDelegateDetails.builder().delegateName(delegateName).build();
    Connector connector =
        KubernetesClusterConfig.builder().credentialType(INHERIT_FROM_DELEGATE).credential(delegateCredential).build();
    KubernetesClusterConfigDTO connectorDTO =
        kubernetesEntityToDTO.createConnectorDTO((KubernetesClusterConfig) connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(((KubernetesDelegateDetailsDTO) connectorDTO.getConfig()).getDelegateName()).isEqualTo(delegateName);
    assertThat(connectorDTO.getKubernetesCredentialType()).isEqualTo(INHERIT_FROM_DELEGATE);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreateK8ClusterConfigDTOForUserNamePassword() {
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
    Connector connector = KubernetesClusterConfig.builder()
                              .credentialType(MANUAL_CREDENTIALS)
                              .credential(kubernetesClusterDetails)
                              .build();
    KubernetesClusterConfigDTO connectorDTO =
        kubernetesEntityToDTO.createConnectorDTO((KubernetesClusterConfig) connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getConfig()).isNotNull();
    assertThat(connectorDTO.getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO = (KubernetesClusterDetailsDTO) connectorDTO.getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    KubernetesUserNamePasswordDTO kubernetesUserNamePasswordDTO =
        (KubernetesUserNamePasswordDTO) credentialDTO.getAuth().getCredentials();
    assertThat(kubernetesUserNamePasswordDTO.getUsername()).isEqualTo(userName);
    assertThat(kubernetesUserNamePasswordDTO.getEncryptedPassword()).isEqualTo(password);
    assertThat(kubernetesUserNamePasswordDTO.getCacert()).isEqualTo(cacert);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreateK8ClusterConfigDTOForClientKeyCert() {
    String clientKey = "encryptedClientKey";
    String clientCert = "encryptedClientCert";
    String clientKeyPhrase = "clientKeyPhrase";
    String clientKeyAlgo = "clientKeyAlgo";
    String masterUrl = "https://abc.com";
    K8sClientKeyCert k8sClientKeyCert = K8sClientKeyCert.builder()
                                            .clientKey(clientKey)
                                            .clientCert(clientCert)
                                            .clientKeyPassphrase(clientKeyPhrase)
                                            .clientKeyAlgo(clientKeyAlgo)
                                            .build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterUrl)
                                                            .authType(KubernetesAuthType.CLIENT_KEY_CERT)
                                                            .auth(k8sClientKeyCert)
                                                            .build();
    Connector connector = KubernetesClusterConfig.builder()
                              .credentialType(MANUAL_CREDENTIALS)
                              .credential(kubernetesClusterDetails)
                              .build();
    KubernetesClusterConfigDTO connectorDTO =
        kubernetesEntityToDTO.createConnectorDTO((KubernetesClusterConfig) connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getConfig()).isNotNull();
    assertThat(connectorDTO.getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO = (KubernetesClusterDetailsDTO) connectorDTO.getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    KubernetesClientKeyCertDTO kubernetesClientKeyCertDTO =
        (KubernetesClientKeyCertDTO) credentialDTO.getAuth().getCredentials();
    assertThat(kubernetesClientKeyCertDTO.getEncryptedClientKey()).isEqualTo(clientKey);
    assertThat(kubernetesClientKeyCertDTO.getEncryptedClientCert()).isEqualTo(clientCert);
    assertThat(kubernetesClientKeyCertDTO.getEncryptedClientKeyPassphrase()).isEqualTo(clientKeyPhrase);
    assertThat(kubernetesClientKeyCertDTO.getClientKeyAlgo()).isEqualTo(clientKeyAlgo);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreateK8ClusterConfigDTOForOIDCConnect() {
    String oidClientId = "encryptedOidcClientId";
    String oidcIssuerUrl = "oidcIssuerUrl";
    String oidcPassword = "encryptedOidcPassword";
    String oidcScopes = "oidcScopes";
    String oidcSecret = "encryptedOidcSecret";
    String oidcUsername = "oidcUsername";
    String masterUrl = "https://abc.com";
    K8sOpenIdConnect k8sOpenIdConnect = K8sOpenIdConnect.builder()
                                            .oidcClientId(oidClientId)
                                            .oidcIssuerUrl(oidcIssuerUrl)
                                            .oidcPassword(oidcPassword)
                                            .oidcScopes(oidcScopes)
                                            .oidcSecret(oidcSecret)
                                            .oidcUsername(oidcUsername)
                                            .build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterUrl)
                                                            .authType(KubernetesAuthType.OPEN_ID_CONNECT)
                                                            .auth(k8sOpenIdConnect)
                                                            .build();
    Connector connector = KubernetesClusterConfig.builder()
                              .credentialType(MANUAL_CREDENTIALS)
                              .credential(kubernetesClusterDetails)
                              .build();
    KubernetesClusterConfigDTO connectorDTO =
        kubernetesEntityToDTO.createConnectorDTO((KubernetesClusterConfig) connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getConfig()).isNotNull();
    assertThat(connectorDTO.getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO = (KubernetesClusterDetailsDTO) connectorDTO.getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    KubernetesOpenIdConnectDTO kubernetesOpenIdConnectDTO =
        (KubernetesOpenIdConnectDTO) credentialDTO.getAuth().getCredentials();
    assertThat(kubernetesOpenIdConnectDTO.getEncryptedOidcClientId()).isEqualTo(oidClientId);
    assertThat(kubernetesOpenIdConnectDTO.getOidcIssuerUrl()).isEqualTo(oidcIssuerUrl);
    assertThat(kubernetesOpenIdConnectDTO.getEncryptedOidcPassword()).isEqualTo(oidcPassword);
    assertThat(kubernetesOpenIdConnectDTO.getOidcScopes()).isEqualTo(oidcScopes);
    assertThat(kubernetesOpenIdConnectDTO.getEncryptedOidcSecret()).isEqualTo(oidcSecret);
    assertThat(kubernetesOpenIdConnectDTO.getOidcUsername()).isEqualTo(oidcUsername);
  }
}