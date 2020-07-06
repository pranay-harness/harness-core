package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.ClientKeyCertK8;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.connector.entities.embedded.kubernetescluster.OpenIdConnectK8;
import io.harness.connector.entities.embedded.kubernetescluster.UserNamePasswordK8;
import io.harness.delegate.beans.connector.k8Connector.ClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.OpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class KubernetesEntityToDTOTest extends ConnectorsBaseTest {
  @Inject @InjectMocks KubernetesEntityToDTO kubernetesEntityToDTO;

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreateK8ClusterConfigDTOForDelegateCredentials() {
    String delegateName = "testDeleagete";
    KubernetesDelegateDetails delegateCredential =
        KubernetesDelegateDetails.builder().delegateName(delegateName).build();
    Connector connector =
        KubernetesClusterConfig.builder().credentialType(INHERIT_FROM_DELEGATE).credential(delegateCredential).build();
    KubernetesClusterConfigDTO connectorDTO = kubernetesEntityToDTO.createK8ClusterConfigDTO(connector);
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
    UserNamePasswordK8 userNamePasswordK8 =
        UserNamePasswordK8.builder().userName(userName).password(password).cacert(cacert).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(userNamePasswordK8)
                                                            .build();
    Connector connector = KubernetesClusterConfig.builder()
                              .credentialType(MANUAL_CREDENTIALS)
                              .credential(kubernetesClusterDetails)
                              .build();
    KubernetesClusterConfigDTO connectorDTO = kubernetesEntityToDTO.createK8ClusterConfigDTO(connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getConfig()).isNotNull();
    assertThat(connectorDTO.getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO = (KubernetesClusterDetailsDTO) connectorDTO.getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    UserNamePasswordDTO userNamePasswordDTO = (UserNamePasswordDTO) credentialDTO.getAuth().getCredentials();
    assertThat(userNamePasswordDTO.getUsername()).isEqualTo(userName);
    assertThat(userNamePasswordDTO.getPassword()).isEqualTo(password);
    assertThat(userNamePasswordDTO.getCacert()).isEqualTo(cacert);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreateK8ClusterConfigDTOForClientKeyCert() {
    String clientKey = "clientKey";
    String clientCert = "clientCert";
    String clientKeyPhrase = "clientKeyPhrase";
    String clientKeyAlgo = "clientKeyAlgo";
    String masterUrl = "https://abc.com";
    ClientKeyCertK8 clientKeyCertK8 = ClientKeyCertK8.builder()
                                          .clientKey(clientKey)
                                          .clientCert(clientCert)
                                          .clientKeyPassphrase(clientKeyPhrase)
                                          .clientKeyAlgo(clientKeyAlgo)
                                          .build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterUrl)
                                                            .authType(KubernetesAuthType.CLIENT_KEY_CERT)
                                                            .auth(clientKeyCertK8)
                                                            .build();
    Connector connector = KubernetesClusterConfig.builder()
                              .credentialType(MANUAL_CREDENTIALS)
                              .credential(kubernetesClusterDetails)
                              .build();
    KubernetesClusterConfigDTO connectorDTO = kubernetesEntityToDTO.createK8ClusterConfigDTO(connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getConfig()).isNotNull();
    assertThat(connectorDTO.getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO = (KubernetesClusterDetailsDTO) connectorDTO.getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    ClientKeyCertDTO clientKeyCertDTO = (ClientKeyCertDTO) credentialDTO.getAuth().getCredentials();
    assertThat(clientKeyCertDTO.getClientKey()).isEqualTo(clientKey);
    assertThat(clientKeyCertDTO.getClientCert()).isEqualTo(clientCert);
    assertThat(clientKeyCertDTO.getClientKeyPassphrase()).isEqualTo(clientKeyPhrase);
    assertThat(clientKeyCertDTO.getClientKeyAlgo()).isEqualTo(clientKeyAlgo);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreateK8ClusterConfigDTOForOIDCConnect() {
    String oidClientId = "oidcClientId";
    String oidcIssuerUrl = "oidcIssuerUrl";
    String oidcPassword = "oidcPassword";
    String oidcScopes = "oidcScopes";
    String oidcSecret = "oidcSecret";
    String oidcUsername = "oidcUsername";
    String masterUrl = "https://abc.com";
    OpenIdConnectK8 openIdConnectK8 = OpenIdConnectK8.builder()
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
                                                            .auth(openIdConnectK8)
                                                            .build();
    Connector connector = KubernetesClusterConfig.builder()
                              .credentialType(MANUAL_CREDENTIALS)
                              .credential(kubernetesClusterDetails)
                              .build();
    KubernetesClusterConfigDTO connectorDTO = kubernetesEntityToDTO.createK8ClusterConfigDTO(connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getConfig()).isNotNull();
    assertThat(connectorDTO.getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO = (KubernetesClusterDetailsDTO) connectorDTO.getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    OpenIdConnectDTO openIdConnectDTO = (OpenIdConnectDTO) credentialDTO.getAuth().getCredentials();
    assertThat(openIdConnectDTO.getOidcClientId()).isEqualTo(oidClientId);
    assertThat(openIdConnectDTO.getOidcIssuerUrl()).isEqualTo(oidcIssuerUrl);
    assertThat(openIdConnectDTO.getOidcPassword()).isEqualTo(oidcPassword);
    assertThat(openIdConnectDTO.getOidcScopes()).isEqualTo(oidcScopes);
    assertThat(openIdConnectDTO.getOidcSecret()).isEqualTo(oidcSecret);
    assertThat(openIdConnectDTO.getOidcUsername()).isEqualTo(oidcUsername);
  }
}