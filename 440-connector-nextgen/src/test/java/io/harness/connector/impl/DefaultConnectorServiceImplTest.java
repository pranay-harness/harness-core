/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.connector.ConnectivityStatus.SUCCESS;
import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.ConnectorsTestBase;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
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
import io.harness.gitsync.clients.YamlGitConfigClient;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.repositories.ConnectorRepository;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

@OwnedBy(DX)
@Slf4j
public class DefaultConnectorServiceImplTest extends ConnectorsTestBase {
  @Mock KubernetesConnectionValidator kubernetesConnectionValidator;
  @Inject ConnectorRepository connectorRepository;
  @Mock private Map<String, ConnectionValidator> connectionValidatorMap;
  @Mock EntitySetupUsageClient entitySetupUsageClient;
  @Mock SecretRefInputValidationHelper secretRefInputValidationHelper;
  @Mock ConnectorEntityReferenceHelper connectorEntityReferenceHelper;
  @Mock GitSyncSdkService gitSyncSdkService;
  @Mock YamlGitConfigClient yamlGitConfigClient;
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
  String updatedName = "updatedName";
  String updatedUserName = "updatedUserName";
  String updatedMasterUrl = "updatedMasterUrl";
  String updatedPasswordIdentifier = "updatedPasswordIdentifier";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    secretRefDataCACert = SecretRefData.builder().identifier(cacertIdentifier).scope(Scope.ACCOUNT).build();
    passwordSecretRef = SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build();
    doNothing().when(secretRefInputValidationHelper).validateTheSecretInput(any(), any());
    doReturn(false).when(gitSyncSdkService).isGitSyncEnabled(anyString(), anyString(), anyString());
  }

  private ConnectorDTO createKubernetesConnectorRequestDTO(String connectorIdentifier, String name) {
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

  private ConnectorResponseDTO createConnector(String connectorIdentifier, String name) {
    ConnectorDTO connectorRequestDTO = createKubernetesConnectorRequestDTO(connectorIdentifier, name);
    return connectorService.create(connectorRequestDTO, accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() {
    ConnectorResponseDTO connectorDTOOutput = createConnector(identifier, name);
    ensureKubernetesConnectorFieldsAreCorrect(connectorDTOOutput);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreateConnectorsWithSameName() {
    ConnectorResponseDTO connectorDTOOutput = createConnector(identifier, name);
    assertThatThrownBy(() -> createConnector("identifier1", name)).isExactlyInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testUpdateConnectorsWithSameName() {
    ConnectorResponseDTO connectorDTOOutput = createConnector(identifier, updatedName);
    assertThatThrownBy(() -> {
      connectorService.update(getUpdatedConnector("differentIdentifier"), accountIdentifier);
    }).isExactlyInstanceOf(InvalidRequestException.class);
  }

  private ConnectorDTO getUpdatedConnector(String identifier) {
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
    return ConnectorDTO.builder()
        .connectorInfo(ConnectorInfoDTO.builder()
                           .name(updatedName)
                           .identifier(identifier)
                           .connectorType(KUBERNETES_CLUSTER)
                           .connectorConfig(k8sClusterConfig)
                           .build())
        .build();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testUpdate() {
    SecretRefData passwordRefData =
        SecretRefData.builder().identifier(updatedPasswordIdentifier).scope(Scope.ACCOUNT).build();
    createConnector(identifier, name);
    ConnectorResponseDTO connectorResponse =
        connectorService.update(getUpdatedConnector(identifier), accountIdentifier);
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
    createConnector(connectorIdentifier1, name + "1");
    createConnector(connectorIdentifier2, name + "2");
    createConnector(connectorIdentifier3, name + "3");
    ArgumentCaptor<Page> connectorsListArgumentCaptor = ArgumentCaptor.forClass(Page.class);
    Page<ConnectorResponseDTO> connectorSummaryDTOSList =
        connectorService.list(0, 100, accountIdentifier, null, null, null, "", "", false, false);
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
    createConnector(identifier, name);
    ConnectorResponseDTO connectorDTO = connectorService.get(accountIdentifier, null, null, identifier).get();
    ensureKubernetesConnectorFieldsAreCorrect(connectorDTO);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testMarkConnectorInvalid() {
    createConnector(identifier, name);
    IdentifierRef identifierRef = IdentifierRef.builder().identifier(identifier).build();
    connectorService.markEntityInvalid(accountIdentifier, identifierRef, "xyz");
    Optional<ConnectorResponseDTO> connectorResponseDTO =
        connectorService.get(accountIdentifier, null, null, identifier);
    assertThat(connectorResponseDTO).isPresent();
    assertThat(connectorResponseDTO.get().getEntityValidityDetails().isValid()).isFalse();
    assertThat(connectorResponseDTO.get().getEntityValidityDetails().getInvalidYaml()).isEqualTo("xyz");
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGetWhenConnectorDoesntExists() {
    createConnector(identifier, name);
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
    createConnector(identifier, name);
    Call<ResponseDTO<Boolean>> request = mock(Call.class);
    try {
      when(request.execute()).thenReturn(Response.success(ResponseDTO.newResponse(false)));
    } catch (IOException ex) {
      log.info("Encountered exception ", ex);
    }
    when(entitySetupUsageClient.isEntityReferenced(any(), any(), any())).thenReturn(request);
    boolean deleted = connectorService.delete(accountIdentifier, null, null, identifier);
    verify(entitySetupUsageClient, times(1)).isEntityReferenced(anyString(), anyString(), any(EntityType.class));
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
    createConnector(identifier, name);
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
    when(kubernetesConnectionValidator.validate(
             (ConnectorConfigDTO) any(), anyString(), any(), anyString(), anyString()))
        .thenReturn(null);
    connectorService.validate(connectorRequestDTO, accountIdentifier);
    verify(kubernetesConnectionValidator, times(1))
        .validate((ConnectorConfigDTO) any(), anyString(), any(), anyString(), anyString());
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testConnection() {
    createConnector(identifier, name);
    when(connectionValidatorMap.get(any())).thenReturn(kubernetesConnectionValidator);
    when(kubernetesConnectionValidator.validate(
             (ConnectorConfigDTO) any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(ConnectorValidationResult.builder().status(SUCCESS).build());
    connectorService.testConnection(accountIdentifier, null, null, identifier);
    verify(kubernetesConnectionValidator, times(1))
        .validate((ConnectorConfigDTO) any(), anyString(), anyString(), anyString(), anyString());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testValidateTheIdentifierIsUnique() {
    createConnector(identifier, name);
    boolean isIdentifierUnique =
        connectorService.validateTheIdentifierIsUnique(accountIdentifier, null, null, identifier);
    assertThat(isIdentifierUnique).isFalse();
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category({UnitTests.class})
  public void testListWithBranchesFlag() {
    String connectorIdentifier1 = "connectorIdentifier1";
    String connectorIdentifier2 = "connectorIdentifier2";
    String connectorIdentifier3 = "connectorIdentifier3";
    createConnector(connectorIdentifier1, name + "1");
    createConnector(connectorIdentifier2, name + "2");
    createConnector(connectorIdentifier3, name + "3");
    doReturn(true).when(gitSyncSdkService).isGitSyncEnabled(anyString(), anyString(), anyString());
    Page<ConnectorResponseDTO> connectorSummaryDTOSList =
        connectorService.list(0, 100, accountIdentifier, null, null, null, "", "", false, true);
    assertThat(connectorSummaryDTOSList.getTotalElements()).isEqualTo(3);
    List<String> connectorIdentifierList =
        connectorSummaryDTOSList.stream()
            .map(connectorSummaryDTO -> connectorSummaryDTO.getConnector().getIdentifier())
            .collect(Collectors.toList());
    assertThat(connectorIdentifierList).contains(connectorIdentifier1);
    assertThat(connectorIdentifierList).contains(connectorIdentifier2);
    assertThat(connectorIdentifierList).contains(connectorIdentifier3);
    Page<ConnectorResponseDTO> connectorSummaryDTOSList_1 =
        connectorService.list(0, 2, accountIdentifier, null, null, null, "", "", false, true);
    assertThat(connectorSummaryDTOSList_1.get().count()).isEqualTo(2L);
    Page<ConnectorResponseDTO> connectorSummaryDTOSList_2 =
        connectorService.list(1, 2, accountIdentifier, null, null, null, "", "", false, true);
    assertThat(connectorSummaryDTOSList_2.get().count()).isEqualTo(1L);
  }
}
