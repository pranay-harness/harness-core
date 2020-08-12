package io.harness.connector.impl;

import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.encryption.SecretRefData.SECRET_DELIMINITER;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorScopeHelper;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.repositories.base.ConnectorRepository;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectorServiceImplTest extends ConnectorsBaseTest {
  @Mock KubernetesConnectionValidator kubernetesConnectionValidator;
  @Mock ConnectorRepository connectorRepository;
  @Mock ConnectorScopeHelper connectorScopeHelper = Mockito.mock(ConnectorScopeHelper.class);
  @Mock private Map<String, ConnectionValidator> connectionValidatorMap;
  @Inject @InjectMocks ConnectorServiceImpl connectorService;

  String userName = "userName";
  String cacertIdentifier = "cacertIdentifier";
  String masterUrl = "https://abc.com";
  String identifier = "identifier";
  String name = "name";
  KubernetesClusterConfig connector;
  String accountIdentifier = "accountIdentifier";
  String passwordIdentifier = "passwordIdentifier";
  String caCertRef = "acc" + SECRET_DELIMINITER + cacertIdentifier;
  @Rule public ExpectedException expectedEx = ExpectedException.none();
  SecretRefData passwordSecretRef;
  SecretRefData secretRefDataCACert;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    secretRefDataCACert = SecretRefData.builder().identifier(cacertIdentifier).scope(Scope.ACCOUNT).build();
    passwordSecretRef = SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build();
  }

  private ConnectorRequestDTO createKubernetesConnectorRequestDTO(String connectorIdentifier) {
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(KubernetesAuthType.USER_PASSWORD)
                                              .credentials(KubernetesUserNamePasswordDTO.builder()
                                                               .username(userName)
                                                               .passwordRef(passwordSecretRef)
                                                               .caCertRef(secretRefDataCACert)
                                                               .build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithDelegateCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    return ConnectorRequestDTO.builder()
        .name(name)
        .identifier(connectorIdentifier)
        .connectorType(KUBERNETES_CLUSTER)
        .connectorConfig(connectorDTOWithDelegateCreds)
        .build();
  }

  private ConnectorDTO createConnector(String connectorIdentifier) {
    ConnectorRequestDTO connectorRequestDTO = createKubernetesConnectorRequestDTO(connectorIdentifier);
    return connectorService.create(connectorRequestDTO, accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() {
    ConnectorDTO connectorDTOOutput = createConnector(identifier);
    ensureKubernetesConnectorFieldsAreCorrect(connectorDTOOutput);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testUpdate() {
    String updatedCACert = "updatedCACert";
    String updatedName = "updatedName";
    String updatedUserName = "updatedUserName";
    String updatedMasterUrl = "updatedMasterUrl";
    String updatedPasswordIdentifier = "updatedPasswordIdentifier";
    createConnector(identifier);
    SecretRefData secretRefDataCACert = SecretRefData.builder().identifier(updatedCACert).scope(Scope.ACCOUNT).build();
    SecretRefData passwordRefData =
        SecretRefData.builder().identifier(updatedPasswordIdentifier).scope(Scope.ACCOUNT).build();
    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(KubernetesAuthType.USER_PASSWORD)
                                              .credentials(KubernetesUserNamePasswordDTO.builder()
                                                               .username(updatedUserName)
                                                               .passwordRef(passwordRefData)
                                                               .caCertRef(secretRefDataCACert)
                                                               .build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePwdCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(updatedMasterUrl).auth(kubernetesAuthDTO).build())
            .build();
    ConnectorRequestDTO newConnectorRequestDTO = ConnectorRequestDTO.builder()
                                                     .name(updatedName)
                                                     .identifier(identifier)
                                                     .connectorType(KUBERNETES_CLUSTER)
                                                     .connectorConfig(connectorDTOWithUserNamePwdCreds)
                                                     .build();

    ConnectorDTO connectorDTOOutput = connectorService.update(newConnectorRequestDTO, accountIdentifier);
    assertThat(connectorDTOOutput).isNotNull();
    assertThat(connectorDTOOutput.getName()).isEqualTo(updatedName);
    assertThat(connectorDTOOutput.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorDTOOutput.getConnectorType()).isEqualTo(KUBERNETES_CLUSTER);
    KubernetesClusterConfigDTO kubernetesCluster = (KubernetesClusterConfigDTO) connectorDTOOutput.getConnectorConfig();
    assertThat(kubernetesCluster).isNotNull();
    assertThat(kubernetesCluster.getConfig()).isNotNull();
    assertThat(kubernetesCluster.getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO = (KubernetesClusterDetailsDTO) kubernetesCluster.getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isEqualTo(updatedMasterUrl);
    assertThat(credentialDTO.getAuth()).isNotNull();
    KubernetesUserNamePasswordDTO userNamePasswordDTO =
        (KubernetesUserNamePasswordDTO) credentialDTO.getAuth().getCredentials();
    assertThat(userNamePasswordDTO.getUsername()).isEqualTo(updatedUserName);
    assertThat(userNamePasswordDTO.getPasswordRef()).isEqualTo(passwordRefData);
    assertThat(userNamePasswordDTO.getCaCertRef()).isEqualTo(secretRefDataCACert);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category({UnitTests.class})
  public void testList() {
    String connectorIdentifier1 = "connectorIdentifier1";
    String connectorIdentifier2 = "connectorIdentifier2";
    String connectorIdentifier3 = "connectorIdentifier3";
    createConnector(connectorIdentifier1);
    createConnector(connectorIdentifier2);
    createConnector(connectorIdentifier3);
    ArgumentCaptor<Page> connectorsListArgumentCaptor = ArgumentCaptor.forClass(Page.class);
    Page<ConnectorSummaryDTO> connectorSummaryDTOSList =
        connectorService.list(null, 0, 100, accountIdentifier, null, null);
    verify(connectorScopeHelper, times(1))
        .createConnectorSummaryListForConnectors(connectorsListArgumentCaptor.capture());
    List<Connector> connectorsList = connectorsListArgumentCaptor.getValue().toList();
    assertThat(connectorsList.size()).isEqualTo(3);
    List<String> connectorIdentifierList = connectorsList.stream()
                                               .map(connectorSummaryDTO -> connectorSummaryDTO.getIdentifier())
                                               .collect(Collectors.toList());
    assertThat(connectorIdentifierList).contains(connectorIdentifier1);
    assertThat(connectorIdentifierList).contains(connectorIdentifier2);
    assertThat(connectorIdentifierList).contains(connectorIdentifier3);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGet() {
    createConnector(identifier);
    ConnectorDTO connectorDTO = connectorService.get(accountIdentifier, null, null, identifier).get();
    ensureKubernetesConnectorFieldsAreCorrect(connectorDTO);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGetWhenConnectorDoesntExists() throws Exception {
    expectedEx.expect(InvalidRequestException.class);
    expectedEx.expectMessage(
        "No connector exists with the identifier identifier in account accountIdentifier, organisation orgIdentifier, project projectIdentifier");

    createConnector(identifier);
    ConnectorDTO connectorDTO =
        connectorService.get(accountIdentifier, "orgIdentifier", "projectIdentifier", identifier).get();
  }

  private void ensureKubernetesConnectorFieldsAreCorrect(ConnectorDTO connectorDTOOutput) {
    assertThat(connectorDTOOutput).isNotNull();
    assertThat(connectorDTOOutput.getName()).isEqualTo(name);
    assertThat(connectorDTOOutput.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorDTOOutput.getConnectorType()).isEqualTo(KUBERNETES_CLUSTER);
    KubernetesClusterConfigDTO kubernetesCluster = (KubernetesClusterConfigDTO) connectorDTOOutput.getConnectorConfig();
    assertThat(kubernetesCluster).isNotNull();
    assertThat(kubernetesCluster.getConfig()).isNotNull();
    assertThat(kubernetesCluster.getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO = (KubernetesClusterDetailsDTO) kubernetesCluster.getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    KubernetesUserNamePasswordDTO kubernetesUserNamePasswordDTO =
        (KubernetesUserNamePasswordDTO) credentialDTO.getAuth().getCredentials();
    assertThat(kubernetesUserNamePasswordDTO.getUsername()).isEqualTo(userName);
    assertThat(kubernetesUserNamePasswordDTO.getPasswordRef()).isEqualTo(passwordSecretRef);
    assertThat(kubernetesUserNamePasswordDTO.getCaCertRef()).isEqualTo(secretRefDataCACert);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDelete() {
    createConnector(identifier);
    boolean deleted = connectorService.delete(accountIdentifier, null, null, identifier);
    assertThat(deleted).isTrue();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDeleteWhenConnectorDoesNotExists() {
    boolean deleted = connectorService.delete(accountIdentifier, null, null, identifier);
    assertThat(deleted).isFalse();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testValidate() {
    String userName = "userName";
    String cacert = "caCertRef";
    String masterUrl = "https://abc.com";
    String identifier = "identifier";
    String name = "name";
    SecretRefData secretRefDataCACert = SecretRefData.builder().identifier(cacert).scope(Scope.ACCOUNT).build();
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(
                KubernetesUserNamePasswordDTO.builder()
                    .username(userName)
                    .passwordRef(SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build())
                    .caCertRef(secretRefDataCACert)
                    .build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithDelegateCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    ConnectorRequestDTO connectorRequestDTO = ConnectorRequestDTO.builder()
                                                  .name(name)
                                                  .identifier(identifier)
                                                  .connectorType(KUBERNETES_CLUSTER)
                                                  .connectorConfig(connectorDTOWithDelegateCreds)
                                                  .build();

    when(connectionValidatorMap.get(any())).thenReturn(kubernetesConnectionValidator);
    connectorService.validate(connectorRequestDTO, "accountId");
    verify(kubernetesConnectionValidator, times(1)).validate(any(), anyString(), any(), anyString());
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testConnection() {
    createConnector(identifier);
    when(connectionValidatorMap.get(any())).thenReturn(kubernetesConnectionValidator);
    connectorService.testConnection(accountIdentifier, null, null, identifier);
    verify(kubernetesConnectionValidator, times(1)).validate(any(), anyString(), anyString(), anyString());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testValidateTheIdentifierIsUnique() {
    createConnector(identifier);
    boolean isIdentifierUnique =
        connectorService.validateTheIdentifierIsUnique(accountIdentifier, null, null, identifier);
    assertThat(isIdentifierUnique).isFalse();
  }
}