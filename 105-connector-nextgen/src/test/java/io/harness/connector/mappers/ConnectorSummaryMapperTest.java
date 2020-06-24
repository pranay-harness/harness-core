package io.harness.connector.mappers;

import static io.harness.connector.common.ConnectorCategory.CLOUD_PROVIDER;
import static io.harness.connector.common.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.connector.common.kubernetes.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsBaseTest;
import io.harness.connector.apis.dtos.connector.ConnectorSummaryDTO;
import io.harness.connector.common.kubernetes.KubernetesAuthType;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterConfig;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterDetails;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.UserNamePasswordK8;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConnectorSummaryMapperTest extends ConnectorsBaseTest {
  @Inject @InjectMocks ConnectorSummaryMapper connectorSummaryMapper;

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void writeConnectorSummaryDTO() {
    String masterURL = "masterURL";
    String userName = "userName";
    String password = "password";
    String cacert = "cacert";
    String description = "description";
    String name = "name";
    String identifier = "identiifier";
    List<String> tags = Arrays.asList("tag1", "tag2");
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
    connector.setDescription(description);
    connector.setName(name);
    connector.setType(KUBERNETES_CLUSTER);
    connector.setIdentifier(identifier);
    connector.setTags(tags);
    connector.setCategories(Collections.singletonList(CLOUD_PROVIDER));
    connector.setCreatedAt(1L);
    connector.setLastModifiedAt(1L);

    ConnectorSummaryDTO connectorSummaryDTO = connectorSummaryMapper.writeConnectorSummaryDTO(connector);

    assertThat(connectorSummaryDTO).isNotNull();
    assertThat(connectorSummaryDTO.getConnectorDetials()).isNotNull();
    assertThat(connectorSummaryDTO.getCategories()).isEqualTo(Collections.singletonList(CLOUD_PROVIDER));
    assertThat(connectorSummaryDTO.getType()).isEqualTo(KUBERNETES_CLUSTER);
    assertThat(connectorSummaryDTO.getCreatedAt()).isGreaterThan(0);
    assertThat(connectorSummaryDTO.getLastModifiedAt()).isGreaterThan(0);
    assertThat(connectorSummaryDTO.getDescription()).isEqualTo(description);
    assertThat(connectorSummaryDTO.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorSummaryDTO.getTags()).isEqualTo(tags);
  }
}