package io.harness.connector.impl;

import static io.harness.connector.common.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.connector.common.kubernetes.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.apis.dtos.K8Connector.KubernetesAuthDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterDetailsDTO;
import io.harness.connector.apis.dtos.K8Connector.UserNamePasswordDTO;
import io.harness.connector.apis.dtos.connector.ConnectorDTO;
import io.harness.connector.apis.dtos.connector.ConnectorRequestDTO;
import io.harness.connector.apis.dtos.connector.ConnectorSummaryDTO;
import io.harness.connector.common.kubernetes.KubernetesAuthType;
import io.harness.connector.services.ConnectorService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;

import java.util.Optional;

public class ConnectorServiceImplTest extends ConnectorsBaseTest {
  @Inject ConnectorService connectorService;
  String userName = "userName";
  String password = "password";
  String cacert = "cacert";
  String masterUrl = "https://abc.com";
  String identifier = "identifier";
  String name = "name";

  private ConnectorDTO createConnector() {
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(UserNamePasswordDTO.builder().username(userName).password(password).cacert(cacert).build())
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
    return connectorService.create(connectorRequestDTO);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() {
    ConnectorDTO connectorDTOOutput = createConnector();
    ensureConnectorFieldsAreCorrect(connectorDTOOutput);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testUpdate() {
    String userName = "userName1";
    String password = "password1";
    String cacert = "cacert1";
    String masterUrl = "https://abc.com1";
    String name = "name1";
    createConnector();
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(UserNamePasswordDTO.builder().username(userName).password(password).cacert(cacert).build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithDelegateCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    ConnectorRequestDTO newConnectorRequestDTO = ConnectorRequestDTO.builder()
                                                     .name(name)
                                                     .identifier(identifier)
                                                     .connectorType(KUBERNETES_CLUSTER)
                                                     .connectorConfig(connectorDTOWithDelegateCreds)
                                                     .build();
    ConnectorDTO connectorDTOOutput = connectorService.update(newConnectorRequestDTO);
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
  @Category(UnitTests.class)
  public void testList() {
    createConnector();
    createConnector();
    createConnector();
    Page<ConnectorSummaryDTO> connectorSummaryDTOS = connectorService.list(null, 0, 100);
    assertThat(connectorSummaryDTOS.getTotalElements()).isEqualTo(3);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGet() {
    createConnector();
    ConnectorDTO connectorDTO = connectorService.get(null, null, null, identifier).get();
    ensureConnectorFieldsAreCorrect(connectorDTO);
  }

  private void ensureConnectorFieldsAreCorrect(ConnectorDTO connectorDTOOutput) {
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
    UserNamePasswordDTO userNamePasswordDTO = (UserNamePasswordDTO) credentialDTO.getAuth().getCredentials();
    assertThat(userNamePasswordDTO.getUsername()).isEqualTo(userName);
    assertThat(userNamePasswordDTO.getPassword()).isEqualTo(password);
    assertThat(userNamePasswordDTO.getCacert()).isEqualTo(cacert);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDelete() {
    createConnector();
    boolean deleted = connectorService.delete(null, null, null, identifier);
    assertThat(deleted).isTrue();
    Optional<ConnectorDTO> connectorDTO = connectorService.get(null, null, null, identifier);
    assertThat(connectorDTO.isPresent()).isFalse();
  }
}