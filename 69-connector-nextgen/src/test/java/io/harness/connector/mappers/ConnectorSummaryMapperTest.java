package io.harness.connector.mappers;

import static io.harness.delegate.beans.connector.ConnectorCategory.CLOUD_PROVIDER;
import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.encryption.SecretRefData.SECRET_DELIMINITER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.apis.dto.k8connector.KubernetesConfigSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.mappers.kubernetesMapper.KubernetesConfigSummaryMapper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectorSummaryMapperTest extends CategoryTest {
  @InjectMocks ConnectorSummaryMapper connectorSummaryMapper;
  @Mock KubernetesConfigSummaryMapper kubernetesConfigSummaryMapper;
  Connector connector;
  String name = "name";
  String description = "description";
  String identifier = "identiifier";
  List<String> tags = Arrays.asList("tag1", "tag2");
  String accountName = "Test Account";
  @Mock private Map<String, ConnectorConfigSummaryDTOMapper> connectorConfigSummaryDTOMapperMap;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    String masterURL = "masterURL";
    String userName = "userName";
    String cacert = "caCertRef";
    String passwordIdentifier = "passwordIdentifier";
    String cacertIdentifier = "cacertIdentifier";
    String passwordRef = "acc" + SECRET_DELIMINITER + passwordIdentifier;
    String caCertRef = "acc" + SECRET_DELIMINITER + cacertIdentifier;
    SecretRefData passcordSecretRef = SecretRefData.builder().identifier(passwordRef).scope(Scope.ACCOUNT).build();
    SecretRefData secretRefDataCACert = SecretRefData.builder().identifier(cacert).scope(Scope.ACCOUNT).build();
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).passwordRef(passwordRef).caCertRef(caCertRef).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    connector = KubernetesClusterConfig.builder()
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

    KubernetesConfigSummaryDTO kubernetesSummary =
        KubernetesConfigSummaryDTO.builder().masterURL("masterURL").delegateName(null).build();
    when(connectorConfigSummaryDTOMapperMap.get(any())).thenReturn(kubernetesConfigSummaryMapper);
    when(kubernetesConfigSummaryMapper.toConnectorConfigSummaryDTO(any())).thenReturn(kubernetesSummary);
  }

  private void assertConnectorSummaryFieldsAreCorrect(ConnectorSummaryDTO connectorSummaryDTO) {
    assertThat(connectorSummaryDTO).isNotNull();
    assertThat(connectorSummaryDTO.getConnectorDetails()).isNotNull();
    assertThat(connectorSummaryDTO.getCategories()).isEqualTo(Collections.singletonList(CLOUD_PROVIDER));
    assertThat(connectorSummaryDTO.getType()).isEqualTo(KUBERNETES_CLUSTER);
    assertThat(connectorSummaryDTO.getCreatedAt()).isGreaterThan(0);
    assertThat(connectorSummaryDTO.getLastModifiedAt()).isGreaterThan(0);
    assertThat(connectorSummaryDTO.getDescription()).isEqualTo(description);
    assertThat(connectorSummaryDTO.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorSummaryDTO.getTags()).isEqualTo(tags);
    assertThat(connectorSummaryDTO.getAccountName()).isEqualTo(accountName);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void writeConnectorSummaryDTOTest() {
    connector.setOrgIdentifier(null);
    connector.setProjectIdentifier(null);
    ConnectorSummaryDTO connectorSummaryDTO =
        connectorSummaryMapper.writeConnectorSummaryDTO(connector, accountName, null, null);
    assertConnectorSummaryFieldsAreCorrect(connectorSummaryDTO);
    assertThat(connectorSummaryDTO.getOrgName()).isBlank();
    assertThat(connectorSummaryDTO.getProjectName()).isBlank();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void writeConnectorSummaryDTOForOrgLevelConnectorTest() {
    String orgIdentifier = "orgIdentifier";
    String orgName = "orgName";
    Map<String, String> orgIdOrgNameMap = new HashMap<String, String>() {
      { put(orgIdentifier, orgName); }
    };
    connector.setScope(Connector.Scope.ORGANIZATION);
    connector.setOrgIdentifier(orgIdentifier);
    connector.setProjectIdentifier(null);
    ConnectorSummaryDTO connectorSummaryDTO =
        connectorSummaryMapper.writeConnectorSummaryDTO(connector, accountName, orgIdOrgNameMap, null);
    assertConnectorSummaryFieldsAreCorrect(connectorSummaryDTO);
    assertThat(connectorSummaryDTO.getOrgName()).isEqualTo(orgName);
    assertThat(connectorSummaryDTO.getProjectName()).isBlank();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void writeConnectorSummaryDTOForProjectLevelConnectorTest() {
    String orgIdentifier = "orgIdentifier";
    String orgName = "orgName";
    Map<String, String> orgIdOrgNameMap = new HashMap<String, String>() {
      { put(orgIdentifier, orgName); }
    };

    String projectIdentifier = "projectIdentifier";
    String projectName = "projectName";
    Map<String, String> projectIdProjectNameMap = new HashMap<String, String>() {
      { put(projectIdentifier, projectName); }
    };

    connector.setScope(Connector.Scope.PROJECT);
    connector.setOrgIdentifier(orgIdentifier);
    connector.setProjectIdentifier(projectIdentifier);

    ConnectorSummaryDTO connectorSummaryDTO = connectorSummaryMapper.writeConnectorSummaryDTO(
        connector, accountName, orgIdOrgNameMap, projectIdProjectNameMap);
    assertConnectorSummaryFieldsAreCorrect(connectorSummaryDTO);
    assertThat(connectorSummaryDTO.getOrgName()).isEqualTo(orgName);
    assertThat(connectorSummaryDTO.getProjectName()).isEqualTo(projectName);
  }
}