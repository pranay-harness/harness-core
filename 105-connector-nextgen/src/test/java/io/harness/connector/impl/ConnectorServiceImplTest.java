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
import io.harness.connector.common.kubernetes.KubernetesAuthType;
import io.harness.connector.services.ConnectorService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConnectorServiceImplTest extends ConnectorsBaseTest {
  @Inject ConnectorService connectorService;

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() {
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    String masterUrl = "https://abc.com";
    String identifier = "identifier";
    String name = "name";
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
    ConnectorDTO connectorDTOOutput = connectorService.create(connectorRequestDTO);
    assertThat(connectorDTOOutput).isNotNull();
    assertThat(connectorRequestDTO.getName()).isEqualTo(name);
    assertThat(connectorRequestDTO.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorRequestDTO.getConnectorType()).isEqualTo(KUBERNETES_CLUSTER);
    KubernetesClusterConfigDTO kubernetesCluster =
        (KubernetesClusterConfigDTO) connectorRequestDTO.getConnectorConfig();
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
}