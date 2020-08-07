package io.harness.connector.mappers;

import static io.harness.connector.entities.Connector.Scope.ACCOUNT;
import static io.harness.connector.entities.Connector.Scope.ORGANIZATION;
import static io.harness.connector.entities.Connector.Scope.PROJECT;
import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.encryption.SecretRefData.SECRET_DELIMINITER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.mappers.kubernetesMapper.KubernetesConfigCastHelper;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
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

import java.util.Map;

public class ConnectorMapperTest extends CategoryTest {
  @InjectMocks private ConnectorMapper connectorMapper;
  @Mock KubernetesConfigCastHelper kubernetesConfigCastHelper;
  @Mock KubernetesDTOToEntity kubernetesDTOToEntity;
  @Mock KubernetesEntityToDTO kubernetesEntityToDTO;
  @Mock private Map<String, ConnectorDTOToEntityMapper> connectorDTOToEntityMapperMap;
  @Mock private Map<String, ConnectorEntityToDTOMapper> connectorEntityToDTOMapperMap;
  String masterURL = "masterURL";
  String userName = "userName";
  String identifier = "identifier";
  String name = "name";
  private String accountIdentifier = "accountIdentifier";
  String passwordIdentifier = "passwordIdentifier";
  SecretRefData secretRefDataCACert;

  @Before
  public void setUp() throws Exception {
    String cacert = "caCertRef";
    secretRefDataCACert = SecretRefData.builder().identifier(cacert).scope(Scope.ACCOUNT).build();
    SecretRefData passwordSecretRefData =
        SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build();
    MockitoAnnotations.initMocks(this);
    when(kubernetesConfigCastHelper.castToKubernetesDelegateCredential(any())).thenCallRealMethod();
    when(kubernetesConfigCastHelper.castToManualKubernetesCredentials(any())).thenCallRealMethod();
    when(kubernetesDTOToEntity.toConnectorEntity(any())).thenCallRealMethod();

    KubernetesAuthDTO kubernetesAuthDTO = KubernetesAuthDTO.builder()
                                              .authType(KubernetesAuthType.USER_PASSWORD)
                                              .credentials(KubernetesUserNamePasswordDTO.builder()
                                                               .username(userName)
                                                               .passwordRef(passwordSecretRefData)
                                                               .caCertRef(secretRefDataCACert)
                                                               .build())
                                              .build();
    KubernetesClusterConfigDTO connectorDTOWithUserNamePassword =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterURL).auth(kubernetesAuthDTO).build())
            .build();
    when(kubernetesEntityToDTO.createConnectorDTO(any())).thenReturn(connectorDTOWithUserNamePassword);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testToConnector() {
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(
                KubernetesUserNamePasswordDTO.builder()
                    .username(userName)
                    .passwordRef(SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build())
                    .caCertRef(secretRefDataCACert)
                    .build())
            .build();
    KubernetesClusterConfigDTO connectorDTOWithDelegateCreds =
        KubernetesClusterConfigDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterURL).auth(kubernetesAuthDTO).build())
            .build();
    ConnectorRequestDTO connectorRequestDTO = ConnectorRequestDTO.builder()
                                                  .name(name)
                                                  .identifier(identifier)
                                                  .connectorType(KUBERNETES_CLUSTER)
                                                  .connectorConfig(connectorDTOWithDelegateCreds)
                                                  .build();
    when(connectorDTOToEntityMapperMap.get(any())).thenReturn(kubernetesDTOToEntity);
    Connector connector = connectorMapper.toConnector(connectorRequestDTO, accountIdentifier);
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
  public void testWriteDTOTest() {
    String passwordIdentifier = "passwordRef";
    String cacertIdentifier = "cacertIdentifier";
    String passwordRef = "acc" + SECRET_DELIMINITER + this.passwordIdentifier;
    String caCertRef = "acc" + SECRET_DELIMINITER + cacertIdentifier;
    SecretRefData passcordSecretRef = SecretRefData.builder().identifier(passwordRef).scope(Scope.ACCOUNT).build();
    SecretRefData secretRefDataCACert =
        SecretRefData.builder().identifier(cacertIdentifier).scope(Scope.ACCOUNT).build();
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).passwordRef(passwordRef).caCertRef(caCertRef).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    Connector connector = KubernetesClusterConfig.builder()
                              .credentialType(MANUAL_CREDENTIALS)
                              .credential(kubernetesClusterDetails)
                              .build();
    connector.setName(name);
    connector.setIdentifier(identifier);
    connector.setType(KUBERNETES_CLUSTER);
    when(connectorEntityToDTOMapperMap.get(any())).thenReturn(kubernetesEntityToDTO);
    ConnectorDTO connectorRequestDTO = connectorMapper.writeDTO(connector);
    assertThat(connectorRequestDTO).isNotNull();
    assertThat(connectorRequestDTO.getName()).isEqualTo(name);
    assertThat(connectorRequestDTO.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorRequestDTO.getConnectorType()).isEqualTo(KUBERNETES_CLUSTER);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void getScopeFromConnectorDTOTest() {
    ConnectorRequestDTO connectorDTO = ConnectorRequestDTO.builder().build();
    Connector.Scope accountScope = connectorMapper.getScopeFromConnectorDTO(connectorDTO);
    assertThat(accountScope).isEqualTo(ACCOUNT);

    connectorDTO.setOrgIdentifier("orgIdentifier");
    Connector.Scope orgScope = connectorMapper.getScopeFromConnectorDTO(connectorDTO);
    assertThat(orgScope).isEqualTo(ORGANIZATION);

    connectorDTO.setProjectIdentifier("projectIdentifier");
    Connector.Scope projectScope = connectorMapper.getScopeFromConnectorDTO(connectorDTO);
    assertThat(projectScope).isEqualTo(PROJECT);
  }
}