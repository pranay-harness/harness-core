package io.harness.connector.jacksontests.kubernetescluster;

import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorRequestWrapper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@Slf4j
public class KubernetesClusterConfigSerializationDeserializationTest extends CategoryTest {
  String userName = "userName";
  String masterUrl = "https://abc.com";
  String connectorIdentifier = "identifier";
  String name = "name";
  String tag1 = "tag1";
  String tag2 = "tag2";
  String description = "description";
  String projectIdentifier = "projectIdentifier";
  String orgIdentifier = "orgIdentifier";
  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
  }

  private KubernetesClusterConfigDTO createKubernetesConnectorRequestDTO() {
    SecretRefData secretRefDataCACert = SecretRefData.builder().identifier("caCertRef").scope(Scope.ACCOUNT).build();
    SecretRefData passwordSecretRef = SecretRefData.builder().identifier("passwordRef").scope(Scope.ACCOUNT).build();
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(
                KubernetesUserNamePasswordDTO.builder().username(userName).passwordRef(passwordSecretRef).build())
            .build();
    KubernetesCredentialDTO k8sCredentials =
        KubernetesCredentialDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    return KubernetesClusterConfigDTO.builder().credential(k8sCredentials).build();
  }

  private ConnectorRequestDTO createConnectorRequestDTOForK8sConnector() {
    KubernetesClusterConfigDTO kubernetesConnectorDTO = createKubernetesConnectorRequestDTO();
    return ConnectorRequestDTO.builder()
        .name(name)
        .identifier(connectorIdentifier)
        .description(description)
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .tags(Arrays.asList(tag1, tag2))
        .connectorType(KUBERNETES_CLUSTER)
        .connectorConfig(kubernetesConnectorDTO)
        .build();
  }

  public static String readFileAsString(String file) {
    try {
      return new String(Files.readAllBytes(Paths.get(file)));
    } catch (Exception ex) {
      Assert.fail("Failed reading the json from " + file + " with error " + ex.getMessage());
      return "";
    }
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testSerializationOfK8sConnector() {
    ConnectorRequestDTO connectorRequestDTO = createConnectorRequestDTOForK8sConnector();
    ConnectorRequestWrapper connectorRequestWrapper =
        ConnectorRequestWrapper.builder().connector(connectorRequestDTO).build();
    String connectorString = "";
    try {
      connectorString = objectMapper.writeValueAsString(connectorRequestWrapper);
    } catch (Exception ex) {
      Assert.fail("Encountered exception while serializing k8s connector " + ex.getMessage());
    }
    String expectedResult = readFileAsString("src/test/resources/kubernetescluster/k8sConnector.json");
    try {
      JsonNode tree1 = objectMapper.readTree(expectedResult);
      JsonNode tree2 = objectMapper.readTree(connectorString);
      logger.info("Expected Connector String: {}", tree1.toString());
      logger.info("Actual Connector String: {}", tree2.toString());
      assertThat(tree1.equals(tree2)).isTrue();
    } catch (Exception ex) {
      Assert.fail("Encountered exception while checking the two k8s json value connector" + ex.getMessage());
    }
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDeserializationOfK8sConnector() {
    String connectorInput = readFileAsString("src/test/resources/kubernetescluster/k8sConnector.json");
    ConnectorRequestWrapper inputConnector = null;
    try {
      inputConnector = objectMapper.readValue(connectorInput, ConnectorRequestWrapper.class);
    } catch (Exception ex) {
      Assert.fail("Encountered exception while deserializing k8s connector " + ex.getMessage());
    }
    ConnectorRequestDTO connectorRequestDTO = createConnectorRequestDTOForK8sConnector();
    ConnectorRequestWrapper connectorRequestWrapper =
        ConnectorRequestWrapper.builder().connector(connectorRequestDTO).build();
    assertThat(inputConnector).isEqualTo(connectorRequestWrapper);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testSerializationOfK8sClusterConfig() {
    KubernetesClusterConfigDTO kubernetesClusterConfig = createKubernetesConnectorRequestDTO();
    String connectorString = "";
    try {
      connectorString = objectMapper.writeValueAsString(kubernetesClusterConfig);
    } catch (Exception ex) {
      Assert.fail("Encountered exception while serializing k8s connector " + ex.getMessage());
    }
    String expectedResult = readFileAsString("src/test/resources/kubernetescluster/k8sClusterConfig.json");
    try {
      JsonNode tree1 = objectMapper.readTree(expectedResult);
      JsonNode tree2 = objectMapper.readTree(connectorString);
      logger.info("Expected Connector String: {}", tree1.toString());
      logger.info("Actual Connector String: {}", tree2.toString());
      assertThat(tree1.equals(tree2)).isTrue();
    } catch (Exception ex) {
      Assert.fail("Encountered exception while checking the two k8s json value connector" + ex.getMessage());
    }
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDeserializationOfK8sClusterConfig() {
    String connectorInput = readFileAsString("src/test/resources/kubernetescluster/k8sClusterConfig.json");
    KubernetesClusterConfigDTO inputConnector = null;
    try {
      inputConnector = objectMapper.readValue(connectorInput, KubernetesClusterConfigDTO.class);
    } catch (Exception ex) {
      Assert.fail("Encountered exception while deserializing k8s connector " + ex.getMessage());
    }
    assertThat(inputConnector).isEqualTo(createKubernetesConnectorRequestDTO());
  }
}
