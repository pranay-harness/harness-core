package io.harness.connector.apis.resource;

import static io.harness.connector.common.kubernetes.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesDelegateDetailsDTO;
import io.harness.connector.apis.dtos.connector.ConnectorDTO;
import io.harness.connector.apis.dtos.connector.ConnectorRequestDTO;
import io.harness.connector.common.ConnectorType;
import io.harness.connector.services.ConnectorService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ConnectorResourceTest extends ConnectorsBaseTest {
  @Inject @InjectMocks private ConnectorResource connectorResource;
  @Mock ConnectorService connectorService;

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void create() {
    ConnectorDTO connectorDTO =
        ConnectorDTO.builder()
            .name("connector")
            .identifier("identifier")
            .connectorType(ConnectorType.KUBERNETES_CLUSTER)
            .connectorConfig(KubernetesClusterConfigDTO.builder()
                                 .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                                 .config(KubernetesDelegateDetailsDTO.builder().delegateName("delegateName").build())
                                 .build())
            .build();
    ConnectorRequestDTO randomConnectorRequestDTO =
        ConnectorRequestDTO.builder()
            .name("connector")
            .identifier("identifier")
            .connectorType(ConnectorType.KUBERNETES_CLUSTER)
            .connectorConfig(KubernetesClusterConfigDTO.builder()
                                 .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                                 .config(KubernetesDelegateDetailsDTO.builder().delegateName("delegateName").build())
                                 .build())
            .build();
    ConnectorDTO connectorRequestDTO = connectorResource.create(randomConnectorRequestDTO);
    when(connectorService.create(any())).thenReturn(connectorDTO);
    assertThat(connectorRequestDTO).isNotNull();
  }
}