package io.harness.connector.mappers;

import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.UserNamePasswordK8;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConnectorMapperTest extends ConnectorsBaseTest {
  @Inject ConnectorMapper connectorMapper;

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToConnector() {
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
    Connector connector = connectorMapper.toConnector(connectorRequestDTO);
    assertThat(connector).isNotNull();
    KubernetesClusterConfig k8Config = (KubernetesClusterConfig) connector;
    assertThat(k8Config.getCredentialType()).isEqualTo(MANUAL_CREDENTIALS);
    assertThat(connector.getName()).isEqualTo(name);
    assertThat(connector.getIdentifier()).isEqualTo(identifier);
    assertThat(connector.getType()).isEqualTo(KUBERNETES_CLUSTER);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testWriteDTO() {
    String masterURL = "masterURL";
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    String identifier = "identifier";
    String name = "name";
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
    connector.setName(name);
    connector.setIdentifier(identifier);
    connector.setType(KUBERNETES_CLUSTER);
    ConnectorDTO connectorRequestDTO = connectorMapper.writeDTO(connector);
    assertThat(connectorRequestDTO).isNotNull();
    assertThat(connectorRequestDTO.getName()).isEqualTo(name);
    assertThat(connectorRequestDTO.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorRequestDTO.getConnectorType()).isEqualTo(KUBERNETES_CLUSTER);
  }
}