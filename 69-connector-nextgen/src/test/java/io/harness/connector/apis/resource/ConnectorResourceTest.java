package io.harness.connector.apis.resource;

import static io.harness.delegate.beans.connector.ConnectorCategory.CLOUD_PROVIDER;
import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.NGPageResponse;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorRequestWrapper;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.apis.dto.ConnectorWrapper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.utils.PageTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

import java.util.Arrays;
import java.util.Optional;

public class ConnectorResourceTest extends CategoryTest {
  @Mock private ConnectorService connectorService;
  @InjectMocks private ConnectorResource connectorResource;
  ConnectorDTO connectorDTO;
  ConnectorRequestDTO randomConnectorRequestDTO;
  ConnectorWrapper connectorWrapper;
  ConnectorRequestWrapper connectorRequestWrapper;
  String accountIdentifier = "accountIdentifier";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    connectorDTO =
        ConnectorDTO.builder()
            .name("connector")
            .identifier("identifier")
            .connectorType(KUBERNETES_CLUSTER)
            .connectorConfig(
                KubernetesClusterConfigDTO.builder()
                    .credential(KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                                    .config(KubernetesDelegateDetailsDTO.builder().delegateName("delegateName").build())
                                    .build())
                    .build())
            .build();
    randomConnectorRequestDTO =
        ConnectorRequestDTO.builder()
            .name("connector")
            .identifier("identifier")
            .connectorType(KUBERNETES_CLUSTER)
            .connectorConfig(
                KubernetesClusterConfigDTO.builder()
                    .credential(KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                                    .config(KubernetesDelegateDetailsDTO.builder().delegateName("delegateName").build())
                                    .build())
                    .build())
            .build();
    connectorWrapper = ConnectorWrapper.builder().connector(connectorDTO).build();
    connectorRequestWrapper = ConnectorRequestWrapper.builder().connector(randomConnectorRequestDTO).build();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void create() {
    doReturn(connectorDTO).when(connectorService).create(any(), any());
    ResponseDTO<ConnectorWrapper> connectorResponseDTO =
        connectorResource.create(connectorRequestWrapper, accountIdentifier);
    Mockito.verify(connectorService, times(1)).create(any(), any());
    assertThat(connectorResponseDTO.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void update() {
    when(connectorService.update(any(), any())).thenReturn(connectorDTO);
    ResponseDTO<ConnectorWrapper> connectorResponseDTO =
        connectorResource.update(connectorRequestWrapper, accountIdentifier);
    Mockito.verify(connectorService, times(1)).update(any(), any());
    assertThat(connectorResponseDTO.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void get() {
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(connectorDTO));
    ConnectorWrapper connectorRequestDTO =
        connectorResource.get("accountIdentifier", "orgIdentifier", "projectIdentifier", "connectorIdentifier")
            .getData();
    Mockito.verify(connectorService, times(1)).get(any(), any(), any(), any());
    assertThat(connectorRequestDTO).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void list() {
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String searchTerm = "searchTerm";
    final Page<ConnectorSummaryDTO> page =
        PageTestUtils.getPage(Arrays.asList(ConnectorSummaryDTO.builder().build()), 1);
    when(connectorService.list(100, 0, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm,
             KUBERNETES_CLUSTER, CLOUD_PROVIDER))
        .thenReturn(page);
    ResponseDTO<NGPageResponse<ConnectorSummaryDTO>> connectorSummaryListResponse = connectorResource.list(
        100, 0, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, KUBERNETES_CLUSTER, CLOUD_PROVIDER);
    Mockito.verify(connectorService, times(1))
        .list(eq(100), eq(0), eq(accountIdentifier), eq(orgIdentifier), eq(projectIdentifier), eq(searchTerm),
            eq(KUBERNETES_CLUSTER), eq(CLOUD_PROVIDER));
    assertThat(connectorSummaryListResponse.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void delete() {
    when(connectorService.delete(any(), any(), any(), any())).thenReturn(true);
    ResponseDTO<Boolean> result =
        connectorResource.delete("accountIdentifier", "orgIdentifier", "projectIdentifier", "connectorIdentifier");
    Mockito.verify(connectorService, times(1)).delete(any(), any(), any(), any());
    assertThat(result.getData()).isTrue();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void validateTheIdentifierIsUniqueTest() {
    when(connectorService.validateTheIdentifierIsUnique(any(), any(), any(), any())).thenReturn(true);
    ResponseDTO<Boolean> result = connectorResource.validateTheIdentifierIsUnique(
        "accountIdentifier", "orgIdentifier", "projectIdentifier", "connectorIdentifier");
    Mockito.verify(connectorService, times(1)).validateTheIdentifierIsUnique(any(), any(), any(), any());
    assertThat(result.getData()).isTrue();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void validateTest() {
    ResponseDTO<ConnectorValidationResult> result =
        connectorResource.validate(connectorRequestWrapper, accountIdentifier);
    Mockito.verify(connectorService, times(1)).validate(eq(randomConnectorRequestDTO), eq(accountIdentifier));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testConnectionResourceTest() {
    ResponseDTO<ConnectorValidationResult> validationResult = connectorResource.testConnection(
        "accountIdentifier", "orgIdentifier", "projectIdentifier", "connectorIdentifier");
    Mockito.verify(connectorService, times(1)).testConnection(any(), any(), any(), any());
  }
}