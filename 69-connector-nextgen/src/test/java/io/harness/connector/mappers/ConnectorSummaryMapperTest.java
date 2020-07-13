package io.harness.connector.mappers;

import static io.harness.delegate.beans.connector.ConnectorCategory.CLOUD_PROVIDER;
import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.apis.dto.k8connector.KubernetesConfigSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.UserNamePasswordK8;
import io.harness.connector.mappers.kubernetesMapper.KubernetesConfigSummaryMapper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConnectorSummaryMapperTest extends CategoryTest {
  @InjectMocks ConnectorSummaryMapper connectorSummaryMapper;
  @Mock KubernetesConfigSummaryMapper kubernetesConfigSummaryMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    KubernetesConfigSummaryDTO kubernetesSummary =
        KubernetesConfigSummaryDTO.builder().masterURL("masterURL").delegateName(null).build();
    when(kubernetesConfigSummaryMapper.createKubernetesConfigSummaryDTO(any())).thenReturn(kubernetesSummary);
  }

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