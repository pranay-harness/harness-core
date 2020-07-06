package io.harness.connector.apis.resource;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.Optional;

public class ConnectorResourceTest extends ConnectorsBaseTest {
  @Mock public ConnectorService connectorService;
  @InjectMocks @Inject public ConnectorResource connectorResource;
  ConnectorDTO connectorDTO;
  ConnectorRequestDTO randomConnectorRequestDTO;
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    connectorDTO =
        ConnectorDTO.builder()
            .name("connector")
            .identifier("identifier")
            .connectorType(ConnectorType.KUBERNETES_CLUSTER)
            .connectorConfig(KubernetesClusterConfigDTO.builder()
                                 .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                                 .config(KubernetesDelegateDetailsDTO.builder().delegateName("delegateName").build())
                                 .build())
            .build();
    randomConnectorRequestDTO =
        ConnectorRequestDTO.builder()
            .name("connector")
            .identifier("identifier")
            .connectorType(ConnectorType.KUBERNETES_CLUSTER)
            .connectorConfig(KubernetesClusterConfigDTO.builder()
                                 .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                                 .config(KubernetesDelegateDetailsDTO.builder().delegateName("delegateName").build())
                                 .build())
            .build();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void create() {
    doReturn(connectorDTO).when(connectorService).create(any());
    ConnectorDTO connectorRequestDTO = connectorResource.create(randomConnectorRequestDTO);
    Mockito.verify(connectorService, times(1)).create(any());
    assertThat(connectorRequestDTO).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void update() {
    when(connectorService.update(any())).thenReturn(connectorDTO);
    ConnectorDTO connectorRequestDTO = connectorResource.update(randomConnectorRequestDTO);
    Mockito.verify(connectorService, times(1)).update(any());
    assertThat(connectorRequestDTO).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void get() {
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(connectorDTO));
    ConnectorDTO connectorRequestDTO =
        connectorResource.get("accountIdentifier", "orgIdentifier", "projectIdentifier", "connectorIdentifier").get();
    Mockito.verify(connectorService, times(1)).get(any(), any(), any(), any());
    assertThat(connectorRequestDTO).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void list() {
    final Page<ConnectorSummaryDTO> page = new PageImpl<>(Arrays.asList(ConnectorSummaryDTO.builder().build()));
    when(connectorService.list(null, 100, 0)).thenReturn(page);
    Page<ConnectorSummaryDTO> connectorSummaryList = connectorResource.list(100, 0);
    Mockito.verify(connectorService, times(1)).list(null, 100, 0);
    assertThat(connectorSummaryList).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void delete() {
    when(connectorService.delete(any(), any(), any(), any())).thenReturn(true);
    Boolean result =
        connectorResource.delete("accountIdentifier", "orgIdentifier", "projectIdentifier", "connectorIdentifier");
    Mockito.verify(connectorService, times(1)).delete(any(), any(), any(), any());
    assertThat(result).isTrue();
  }
}