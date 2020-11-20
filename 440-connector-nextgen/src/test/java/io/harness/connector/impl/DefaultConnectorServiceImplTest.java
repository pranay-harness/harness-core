package io.harness.connector.impl;

import static io.harness.delegate.beans.connector.ConnectorCategory.CLOUD_PROVIDER;
import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsTestBase;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.repositories.base.ConnectorRepository;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class DefaultConnectorServiceImplTest extends ConnectorsTestBase {
  @Mock KubernetesConnectionValidator kubernetesConnectionValidator;
  @Mock ConnectorRepository connectorRepository;
  @Mock private Map<String, ConnectionValidator> connectionValidatorMap;
  @Mock EntitySetupUsageClient entitySetupUsageClient;
  @Inject @InjectMocks DefaultConnectorServiceImpl connectorService;

  String userName = "userName";
  String cacertIdentifier = "cacertIdentifier";
  String masterUrl = "https://abc.com";
  String identifier = "identifier";
  String name = "name";
  KubernetesClusterConfig connector;
  String accountIdentifier = "accountIdentifier";
  String passwordIdentifier = "passwordIdentifier";
  @Rule public ExpectedException expectedEx = ExpectedException.none();
  SecretRefData passwordSecretRef;
  SecretRefData secretRefDataCACert;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    secretRefDataCACert = SecretRefData.builder().identifier(cacertIdentifier).scope(Scope.ACCOUNT).build();
    passwordSecretRef = SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build();
  }

  private ConnectorDTO createKubernetesConnectorRequestDTO(String connectorIdentifier) {
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(
                KubernetesUserNamePasswordDTO.builder().username(userName).passwordRef(passwordSecretRef).build())
            .build();
    KubernetesCredentialDTO connectorDTOWithDelegateCreds =
        KubernetesCredentialDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    KubernetesClusterConfigDTO k8sClusterConfig =
        KubernetesClusterConfigDTO.builder().credential(connectorDTOWithDelegateCreds).build();
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder()
                                         .name(name)
                                         .identifier(connectorIdentifier)
                                         .connectorType(KUBERNETES_CLUSTER)
                                         .connectorConfig(k8sClusterConfig)
                                         .build();
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  private ConnectorResponseDTO createConnector(String connectorIdentifier) {
    ConnectorDTO connectorRequestDTO = createKubernetesConnectorRequestDTO(connectorIdentifier);
    return connectorService.create(connectorRequestDTO, accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() {
    ConnectorResponseDTO connectorDTOOutput = createConnector(identifier);
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
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(
                KubernetesUserNamePasswordDTO.builder().username(updatedUserName).passwordRef(passwordRefData).build())
            .build();
    KubernetesCredentialDTO connectorDTOWithUserNamePwdCreds =
        KubernetesCredentialDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(updatedMasterUrl).auth(kubernetesAuthDTO).build())
            .build();
    KubernetesClusterConfigDTO k8sClusterConfig =
        KubernetesClusterConfigDTO.builder().credential(connectorDTOWithUserNamePwdCreds).build();
    ConnectorDTO connector = ConnectorDTO.builder()
                                 .connectorInfo(ConnectorInfoDTO.builder()
                                                    .name(updatedName)
                                                    .identifier(identifier)
                                                    .connectorType(KUBERNETES_CLUSTER)
                                                    .connectorConfig(k8sClusterConfig)
                                                    .build())
                                 .build();

    ConnectorResponseDTO connectorResponse = connectorService.update(connector, accountIdentifier);
    ConnectorInfoDTO connectorInfo = connectorResponse.getConnector();
    assertThat(connectorInfo).isNotNull();
    assertThat(connectorInfo.getName()).isEqualTo(updatedName);
    assertThat(connectorInfo.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorInfo.getConnectorType()).isEqualTo(KUBERNETES_CLUSTER);
    KubernetesClusterConfigDTO kubernetesCluster = (KubernetesClusterConfigDTO) connectorInfo.getConnectorConfig();
    assertThat(kubernetesCluster).isNotNull();
    assertThat(kubernetesCluster.getCredential().getConfig()).isNotNull();
    assertThat(kubernetesCluster.getCredential().getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO =
        (KubernetesClusterDetailsDTO) kubernetesCluster.getCredential().getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isEqualTo(updatedMasterUrl);
    assertThat(credentialDTO.getAuth()).isNotNull();
    KubernetesUserNamePasswordDTO userNamePasswordDTO =
        (KubernetesUserNamePasswordDTO) credentialDTO.getAuth().getCredentials();
    assertThat(userNamePasswordDTO.getUsername()).isEqualTo(updatedUserName);
    assertThat(userNamePasswordDTO.getPasswordRef()).isEqualTo(passwordRefData);
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
    Page<ConnectorResponseDTO> connectorSummaryDTOSList =
        connectorService.list(0, 100, accountIdentifier, null, null, null, KUBERNETES_CLUSTER, CLOUD_PROVIDER);
    assertThat(connectorSummaryDTOSList.getTotalElements()).isEqualTo(3);
    List<String> connectorIdentifierList =
        connectorSummaryDTOSList.stream()
            .map(connectorSummaryDTO -> connectorSummaryDTO.getConnector().getIdentifier())
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
    ConnectorResponseDTO connectorDTO = connectorService.get(accountIdentifier, null, null, identifier).get();
    ensureKubernetesConnectorFieldsAreCorrect(connectorDTO);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGetWhenConnectorDoesntExists() {
    createConnector(identifier);
    Optional<ConnectorResponseDTO> connectorDTO =
        connectorService.get(accountIdentifier, "orgIdentifier", "projectIdentifier", identifier);
    assertThat(connectorDTO.isPresent()).isFalse();
  }

  private void ensureKubernetesConnectorFieldsAreCorrect(ConnectorResponseDTO connectorResponse) {
    ConnectorInfoDTO connectorInfo = connectorResponse.getConnector();
    assertThat(connectorInfo).isNotNull();
    assertThat(connectorInfo.getName()).isEqualTo(name);
    assertThat(connectorInfo.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorInfo.getConnectorType()).isEqualTo(KUBERNETES_CLUSTER);
    KubernetesClusterConfigDTO kubernetesCluster = (KubernetesClusterConfigDTO) connectorInfo.getConnectorConfig();
    assertThat(kubernetesCluster).isNotNull();
    assertThat(kubernetesCluster.getCredential().getConfig()).isNotNull();
    assertThat(kubernetesCluster.getCredential().getKubernetesCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    KubernetesClusterDetailsDTO credentialDTO =
        (KubernetesClusterDetailsDTO) kubernetesCluster.getCredential().getConfig();
    assertThat(credentialDTO).isNotNull();
    assertThat(credentialDTO.getMasterUrl()).isNotNull();
    KubernetesUserNamePasswordDTO kubernetesUserNamePasswordDTO =
        (KubernetesUserNamePasswordDTO) credentialDTO.getAuth().getCredentials();
    assertThat(kubernetesUserNamePasswordDTO.getUsername()).isEqualTo(userName);
    assertThat(kubernetesUserNamePasswordDTO.getPasswordRef()).isEqualTo(passwordSecretRef);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDelete() {
    createConnector(identifier);
    Call<ResponseDTO<Boolean>> request = mock(Call.class);
    try {
      when(request.execute()).thenReturn(Response.success(ResponseDTO.newResponse(false)));
    } catch (IOException ex) {
      log.info("Encountered exception ", ex);
    }
    when(entitySetupUsageClient.isEntityReferenced(any(), any())).thenReturn(request);
    boolean deleted = connectorService.delete(accountIdentifier, null, null, identifier);
    verify(entitySetupUsageClient, times(1)).isEntityReferenced(anyString(), anyString());
    assertThat(deleted).isTrue();
  }

  @Test(expected = InvalidRequestException.class)
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
                    .build())
            .build();
    KubernetesCredentialDTO connectorDTOWithDelegateCreds =
        KubernetesCredentialDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    KubernetesClusterConfigDTO k8sClusterConfig =
        KubernetesClusterConfigDTO.builder().credential(connectorDTOWithDelegateCreds).build();
    ConnectorDTO connectorRequestDTO = ConnectorDTO.builder()
                                           .connectorInfo(ConnectorInfoDTO.builder()
                                                              .name(name)
                                                              .identifier(identifier)
                                                              .connectorType(KUBERNETES_CLUSTER)
                                                              .connectorConfig(k8sClusterConfig)
                                                              .build())
                                           .build();

    when(connectionValidatorMap.get(any())).thenReturn(kubernetesConnectionValidator);
    when(kubernetesConnectionValidator.validate(any(), anyString(), any(), anyString())).thenReturn(null);
    connectorService.validate(connectorRequestDTO, "accountId");
    verify(kubernetesConnectionValidator, times(1)).validate(any(), anyString(), any(), anyString());
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testConnection() {
    createConnector(identifier);
    when(connectionValidatorMap.get(any())).thenReturn(kubernetesConnectionValidator);
    when(kubernetesConnectionValidator.validate(any(), anyString(), anyString(), anyString()))
        .thenReturn(ConnectorValidationResult.builder().valid(true).build());
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
