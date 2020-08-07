package io.harness.connector.impl;

import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.encryption.SecretRefData.SECRET_DELIMINITER;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorFilterHelper;
import io.harness.connector.ConnectorScopeHelper;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectorServiceImplTest extends CategoryTest {
  // todo @deepak This tests should use actual fake mongo and not category test
  @Mock KubernetesConnectionValidator kubernetesConnectionValidator;
  @Mock ConnectorMapper connectorMapper;
  @Mock ConnectorRepository connectorRepository;
  @InjectMocks ConnectorServiceImpl connectorService;
  @Mock private Map<String, ConnectionValidator> connectionValidatorMap;
  @Mock ConnectorFilterHelper connectorFilterHelper;
  @Mock ConnectorScopeHelper connectorScopeHelper;

  String userName = "userName";
  String cacertIdentifier = "cacertIdentifier";
  String masterUrl = "https://abc.com";
  String identifier = "identifier";
  String name = "name";
  ConnectorRequestDTO connectorRequestDTO;
  ConnectorRequestDTO splunkConnectorRequestDTO;
  ConnectorDTO connectorDTO;
  ConnectorDTO splunkConnectorDTO;
  KubernetesClusterConfig connector;
  SplunkConnector splunkConnector;
  String accountIdentifier = "accountIdentifier";
  String passwordIdentifier = "passwordIdentifier";
  String passwordRef = "acc" + SECRET_DELIMINITER + passwordIdentifier;
  String caCertRef = "acc" + SECRET_DELIMINITER + cacertIdentifier;
  String password = "password";
  @Rule public ExpectedException expectedEx = ExpectedException.none();
  SecretRefData passwordSecretRef;
  SecretRefData secretRefDataCACert;

  // todo @deepak: Make this tests use ConnectorBaseTests instead of category tests
  @Before
  public void setUp() throws Exception {
    passwordSecretRef = SecretRefData.builder().identifier(passwordRef).scope(Scope.ACCOUNT).build();
    secretRefDataCACert = SecretRefData.builder().identifier(cacertIdentifier).scope(Scope.ACCOUNT).build();
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).passwordRef(passwordRef).caCertRef(caCertRef).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterUrl)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    connector = KubernetesClusterConfig.builder()
                    .credential(kubernetesClusterDetails)
                    .credentialType(MANUAL_CREDENTIALS)
                    .build();
    connector.setType(KUBERNETES_CLUSTER);
    connector.setIdentifier(identifier);
    connector.setName(name);
    MockitoAnnotations.initMocks(this);

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
    connectorRequestDTO = ConnectorRequestDTO.builder()
                              .name(name)
                              .identifier(identifier)
                              .connectorType(KUBERNETES_CLUSTER)
                              .connectorConfig(connectorDTOWithDelegateCreds)
                              .build();

    connectorDTO = ConnectorDTO.builder()
                       .name(name)
                       .identifier(identifier)
                       .connectorType(KUBERNETES_CLUSTER)
                       .connectorConfig(connectorDTOWithDelegateCreds)
                       .build();

    when(connectorRepository.save(any())).thenReturn(connector);
    when(connectorMapper.writeDTO(any())).thenReturn(connectorDTO);

    when(connectorMapper.toConnector(any(), any())).thenReturn(KubernetesClusterConfig.builder().build());
    doCallRealMethod().when(connectorFilterHelper).createCriteriaFromConnectorFilter(anyObject(), anyString());

    when(connectorRepository.save(splunkConnector)).thenReturn(splunkConnector);
    when(connectorMapper.writeDTO(splunkConnector)).thenReturn(splunkConnectorDTO);
    when(connectorMapper.toConnector(splunkConnectorRequestDTO, accountIdentifier)).thenReturn(splunkConnector);
  }

  private ConnectorDTO createConnector() {
    return connectorService.create(connectorRequestDTO, accountIdentifier);
  }

  private ConnectorDTO createSplunkConnector() {
    return connectorService.create(splunkConnectorRequestDTO, accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() {
    ConnectorDTO connectorDTOOutput = createConnector();
    ensureKubernetesConnectorFieldsAreCorrect(connectorDTOOutput);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testUpdate() {
    String userName = "userName1";
    String cacert = "cacert1";
    String masterUrl = "https://abc.com1";
    String name = "name1";
    SecretRefData secretRefDataCACert = SecretRefData.builder().identifier(cacert).scope(Scope.ACCOUNT).build();
    createConnector();
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
    KubernetesClusterConfigDTO connectorDTOWithUserNamePwdCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    ConnectorRequestDTO newConnectorRequestDTO = ConnectorRequestDTO.builder()
                                                     .name(name)
                                                     .identifier(identifier)
                                                     .connectorType(KUBERNETES_CLUSTER)
                                                     .connectorConfig(connectorDTOWithUserNamePwdCreds)
                                                     .build();

    ConnectorDTO updatedConnectorDTO = ConnectorDTO.builder()
                                           .name(name)
                                           .identifier(identifier)
                                           .connectorType(KUBERNETES_CLUSTER)
                                           .connectorConfig(connectorDTOWithUserNamePwdCreds)
                                           .build();

    when(connectorRepository.findByFullyQualifiedIdentifier(anyString())).thenReturn(Optional.of(connector));
    SecretRefData passcordSecretRef = SecretRefData.builder().identifier(passwordRef).scope(Scope.ACCOUNT).build();
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).passwordRef(passwordRef).caCertRef(caCertRef).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterUrl)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    Connector updatedConnector = KubernetesClusterConfig.builder()
                                     .credential(kubernetesClusterDetails)
                                     .credentialType(MANUAL_CREDENTIALS)
                                     .build();
    updatedConnector.setType(KUBERNETES_CLUSTER);
    updatedConnector.setIdentifier(identifier);
    updatedConnector.setName(name);

    when(connectorRepository.save(any())).thenReturn(updatedConnector);
    when(connectorMapper.writeDTO(any())).thenReturn(updatedConnectorDTO);
    ConnectorDTO connectorDTOOutput = connectorService.update(newConnectorRequestDTO, accountIdentifier);
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
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category({UnitTests.class})
  public void testList() {
    when(connectorRepository.findAll(any(), any())).thenReturn(new PageImpl<>(Arrays.asList(connector)));
    Page<ConnectorSummaryDTO> connectorSummaryDTOS =
        new PageImpl<>(Arrays.asList(ConnectorSummaryDTO.builder().build()));
    doReturn(connectorSummaryDTOS).when(connectorScopeHelper).createConnectorSummaryListForConnectors(any());
    Page<ConnectorSummaryDTO> connectorSummaryDTOSList = connectorService.list(null, 0, 100, accountIdentifier);
    assertThat(connectorSummaryDTOSList.getTotalElements()).isEqualTo(1);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGet() {
    createConnector();
    when(connectorRepository.findByFullyQualifiedIdentifier(anyString())).thenReturn(Optional.of(connector));
    ConnectorDTO connectorDTO = connectorService.get(null, null, null, identifier).get();
    ensureKubernetesConnectorFieldsAreCorrect(connectorDTO);
  }

  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGetWhenConnectorDoesntExists() throws Exception {
    expectedEx.expect(InvalidRequestException.class);
    expectedEx.expectMessage(
        "No connector exists with the identifier identifier in account accountIdentifier, organisation orgIdentifier, project projectIdentifier");

    createConnector();
    when(connectorRepository.findByFullyQualifiedIdentifier(anyString())).thenReturn(Optional.empty());
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
    when(connectorRepository.deleteByFullyQualifiedIdentifier(anyString())).thenReturn(1L);
    boolean deleted = connectorService.delete(null, null, null, identifier);
    assertThat(deleted).isTrue();
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
    when(connectorRepository.findByFullyQualifiedIdentifier(anyString())).thenReturn(Optional.of(connector));
    when(connectionValidatorMap.get(any())).thenReturn(kubernetesConnectionValidator);
    connectorService.testConnection("accountIdentifier", "orgIdentifier", "projectIdenditifer", "identifier");
    verify(kubernetesConnectionValidator, times(1)).validate(any(), anyString(), anyString(), anyString());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testValidateTheIdentifierIsUnique() {
    when(connectorRepository.existsByFullyQualifiedIdentifier(anyString())).thenReturn(true);
    boolean isIdentifierUnique = connectorService.validateTheIdentifierIsUnique(null, null, null, identifier);
    assertThat(isIdentifierUnique).isFalse();
  }
}