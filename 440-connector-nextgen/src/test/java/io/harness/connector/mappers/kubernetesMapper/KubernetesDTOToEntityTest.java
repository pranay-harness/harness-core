package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.CLIENT_KEY_CERT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.OPEN_ID_CONNECT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.SERVICE_ACCOUNT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.USER_PASSWORD;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.K8sClientKeyCert;
import io.harness.connector.entities.embedded.kubernetescluster.K8sOpenIdConnect;
import io.harness.connector.entities.embedded.kubernetescluster.K8sServiceAccount;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.service.SecretRefService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class KubernetesDTOToEntityTest extends CategoryTest {
  @InjectMocks KubernetesDTOToEntity kubernetesDTOToEntity;
  @Mock SecretRefService secretRefService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForDelegateCredentials() {
    String delegateName = "testDelegate";
    KubernetesClusterConfigDTO connectorDTOWithDelegateCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                            .config(KubernetesDelegateDetailsDTO.builder().delegateName(delegateName).build())
                            .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(
        connectorDTOWithDelegateCreds, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(INHERIT_FROM_DELEGATE);
    KubernetesDelegateDetails kubernetesCredential = (KubernetesDelegateDetails) k8Config.getCredential();
    assertThat(kubernetesCredential.getDelegateName()).isEqualTo(delegateName);
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForManualWhenWrongCredTypeGiven() {
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePasswordCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                            .config(KubernetesClusterDetailsDTO.builder().build())
                            .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(connectorDTOWithUserNamePasswordCreds, ngAccess);
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForDelegateWhenWrongCredTypeGiven() {
    KubernetesClusterConfigDTO connectorDTOWithUserNamePasswordCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(MANUAL_CREDENTIALS)
                            .config(KubernetesDelegateDetailsDTO.builder().build())
                            .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(
        connectorDTOWithUserNamePasswordCreds, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForUserNamePasswordCredential() {
    String userName = "userName";
    String passwordIdentifier = "passwordIdentifer";
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();

    SecretRefData passwordSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(passwordSecretRef, ngAccess))
        .thenReturn(passwordSecretRef.toSecretRefStringValue());

    String masterUrl = "https://abc.com";
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(
                KubernetesUserNamePasswordDTO.builder().username(userName).passwordRef(passwordSecretRef).build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePasswordCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(
                KubernetesCredentialDTO.builder()
                    .kubernetesCredentialType(MANUAL_CREDENTIALS)
                    .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
                    .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(connectorDTOWithUserNamePasswordCreds, ngAccess);
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetails kubernetesClusterDetails = (KubernetesClusterDetails) k8Config.getCredential();
    assertThat(kubernetesClusterDetails.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(kubernetesClusterDetails.getAuthType()).isEqualTo(USER_PASSWORD);
    K8sUserNamePassword kubernetesCredential = (K8sUserNamePassword) kubernetesClusterDetails.getAuth();
    assertThat(kubernetesCredential.getUserName()).isEqualTo(userName);
    assertThat(kubernetesCredential.getPasswordRef()).isEqualTo(passwordSecretRef.toSecretRefStringValue());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForClientKeyCert() {
    String clientKeyIdentifier = "clientKeyRef";
    String clientCertIdentifier = "clientCertRef";
    String clientKeyPhraseIdentifier = "clientKeyPhrase";
    String clientKeyAlgo = "clientKeyAlgo";
    String masterUrl = "https://abc.com";
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();

    SecretRefData clientKeySecret =
        SecretRefData.builder().identifier(clientKeyIdentifier).scope(Scope.ACCOUNT).build();
    when(secretRefService.validateAndGetSecretConfigString(clientKeySecret, ngAccess))
        .thenReturn(clientKeySecret.toSecretRefStringValue());

    SecretRefData clientCertSecret =
        SecretRefData.builder().identifier(clientCertIdentifier).scope(Scope.ACCOUNT).build();
    when(secretRefService.validateAndGetSecretConfigString(clientCertSecret, ngAccess))
        .thenReturn(clientCertSecret.toSecretRefStringValue());

    SecretRefData clientKeyPassPhraseSecret =
        SecretRefData.builder().identifier(clientKeyPhraseIdentifier).scope(Scope.ACCOUNT).build();
    when(secretRefService.validateAndGetSecretConfigString(clientKeyPassPhraseSecret, ngAccess))
        .thenReturn(clientKeyPassPhraseSecret.toSecretRefStringValue());

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
            .credential(
                KubernetesCredentialDTO.builder()
                    .kubernetesCredentialType(MANUAL_CREDENTIALS)
                    .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
                    .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(
        connectorDTOWithClientKeyCreds, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetails kubernetesClusterDetails = (KubernetesClusterDetails) k8Config.getCredential();
    assertThat(kubernetesClusterDetails.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(kubernetesClusterDetails.getAuthType()).isEqualTo(CLIENT_KEY_CERT);
    K8sClientKeyCert kubernetesCredential = (K8sClientKeyCert) kubernetesClusterDetails.getAuth();
    assertThat(kubernetesCredential.getClientKeyRef()).isEqualTo(clientKeySecret.toSecretRefStringValue());
    assertThat(kubernetesCredential.getClientCertRef()).isEqualTo(clientCertSecret.toSecretRefStringValue());
    assertThat(kubernetesCredential.getClientKeyPassphraseRef())
        .isEqualTo(clientKeyPassPhraseSecret.toSecretRefStringValue());
    assertThat(kubernetesCredential.getClientKeyAlgo()).isEqualTo(clientKeyAlgo);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForOIDCConnect() {
    String oidClientIdIdentifier = "oidcClientIdRef";
    String oidcIssuerUrl = "oidcIssuerUrl";
    String oidcPasswordIdentifier = "oidcPasswordRef";
    String oidcScopes = "oidcScopes";
    String oidcSecretIdentifier = "oidcSecretRef";
    String oidcUsername = "oidcUsername";
    String masterUrl = "https://abc.com";
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();

    SecretRefData oidcCleintId = SecretRefData.builder().identifier(oidClientIdIdentifier).scope(Scope.ACCOUNT).build();
    when(secretRefService.validateAndGetSecretConfigString(oidcCleintId, ngAccess))
        .thenReturn(oidcCleintId.toSecretRefStringValue());

    SecretRefData oidcPassword =
        SecretRefData.builder().identifier(oidcPasswordIdentifier).scope(Scope.ACCOUNT).build();
    when(secretRefService.validateAndGetSecretConfigString(oidcPassword, ngAccess))
        .thenReturn(oidcPassword.toSecretRefStringValue());

    SecretRefData oidcSecret = SecretRefData.builder().identifier(oidcSecretIdentifier).scope(Scope.ACCOUNT).build();
    when(secretRefService.validateAndGetSecretConfigString(oidcSecret, ngAccess))
        .thenReturn(oidcSecret.toSecretRefStringValue());

    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(OPEN_ID_CONNECT)
                                              .credentials(KubernetesOpenIdConnectDTO.builder()
                                                               .oidcClientIdRef(oidcCleintId)
                                                               .oidcIssuerUrl(oidcIssuerUrl)
                                                               .oidcPasswordRef(oidcPassword)
                                                               .oidcScopes(oidcScopes)
                                                               .oidcSecretRef(oidcSecret)
                                                               .oidcUsername(oidcUsername)
                                                               .build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithOpenIdConnectCred =
        KubernetesClusterConfigDTO.builder()
            .credential(
                KubernetesCredentialDTO.builder()
                    .kubernetesCredentialType(MANUAL_CREDENTIALS)
                    .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
                    .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(
        connectorDTOWithOpenIdConnectCred, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetails kubernetesClusterDetails = (KubernetesClusterDetails) k8Config.getCredential();
    assertThat(kubernetesClusterDetails.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(kubernetesClusterDetails.getAuthType()).isEqualTo(OPEN_ID_CONNECT);
    K8sOpenIdConnect kubernetesCredential = (K8sOpenIdConnect) kubernetesClusterDetails.getAuth();
    assertThat(kubernetesCredential.getOidcClientIdRef()).isEqualTo(oidcCleintId.toSecretRefStringValue());
    assertThat(kubernetesCredential.getOidcIssuerUrl()).isEqualTo(oidcIssuerUrl);
    assertThat(kubernetesCredential.getOidcPasswordRef()).isEqualTo(oidcPassword.toSecretRefStringValue());
    assertThat(kubernetesCredential.getOidcScopes()).isEqualTo(oidcScopes);
    assertThat(kubernetesCredential.getOidcSecretRef()).isEqualTo(oidcSecret.toSecretRefStringValue());
    assertThat(kubernetesCredential.getOidcUsername()).isEqualTo(oidcUsername);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToKubernetesClusterConfigForServiceAccount() {
    String serviceAccountKey = "serviceAccountKey";
    String masterUrl = "https://abc.com";
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();

    SecretRefData serviceAccountTokenRef =
        SecretRefData.builder().identifier(serviceAccountKey).scope(Scope.ACCOUNT).build();
    when(secretRefService.validateAndGetSecretConfigString(serviceAccountTokenRef, ngAccess))
        .thenReturn(serviceAccountTokenRef.toSecretRefStringValue());

    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(SERVICE_ACCOUNT)
            .credentials(KubernetesServiceAccountDTO.builder().serviceAccountTokenRef(serviceAccountTokenRef).build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithServiceAccountCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(
                KubernetesCredentialDTO.builder()
                    .kubernetesCredentialType(MANUAL_CREDENTIALS)
                    .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
                    .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(
        connectorDTOWithServiceAccountCreds, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetails kubernetesClusterDetails = (KubernetesClusterDetails) k8Config.getCredential();
    assertThat(kubernetesClusterDetails.getMasterUrl()).isEqualTo(masterUrl);
    assertThat(kubernetesClusterDetails.getAuthType()).isEqualTo(SERVICE_ACCOUNT);
    K8sServiceAccount kubernetesCredential = (K8sServiceAccount) kubernetesClusterDetails.getAuth();
    assertThat(kubernetesCredential.getServiceAcccountTokenRef())
        .isEqualTo(serviceAccountTokenRef.toSecretRefStringValue());
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToManualConfigWhenUserGaveWrongTypeForServiceAccountAuth() {
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(USER_PASSWORD)
                                              .credentials(KubernetesServiceAccountDTO.builder().build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePasswordCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(MANUAL_CREDENTIALS)
                            .config(KubernetesClusterDetailsDTO.builder().auth(kubernetesAuthDTO).build())
                            .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(connectorDTOWithUserNamePasswordCreds, ngAccess);
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToManualConfigWhenUserGaveWrongTypeForUserNamePwdAuth() {
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(SERVICE_ACCOUNT)
                                              .credentials(KubernetesUserNamePasswordDTO.builder().build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePasswordCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(MANUAL_CREDENTIALS)
                            .config(KubernetesClusterDetailsDTO.builder().auth(kubernetesAuthDTO).build())
                            .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(
        connectorDTOWithUserNamePasswordCreds, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToManualConfigWhenUserGaveWrongTypeForClientKeyAuth() {
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(OPEN_ID_CONNECT)
                                              .credentials(KubernetesClientKeyCertDTO.builder().build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePasswordCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(MANUAL_CREDENTIALS)
                            .config(KubernetesClusterDetailsDTO.builder().auth(kubernetesAuthDTO).build())
                            .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(
        connectorDTOWithUserNamePasswordCreds, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToManualConfigWhenUserGaveWrongTypeForOIDCAuth() {
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(CLIENT_KEY_CERT)
                                              .credentials(KubernetesOpenIdConnectDTO.builder().build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePasswordCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(MANUAL_CREDENTIALS)
                            .config(KubernetesClusterDetailsDTO.builder().auth(kubernetesAuthDTO).build())
                            .build())
            .build();
    Connector connector = kubernetesDTOToEntity.toConnectorEntity(
        connectorDTOWithUserNamePasswordCreds, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
  }
}
