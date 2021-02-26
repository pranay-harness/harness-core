package io.harness.connector.jacksontests.connector;

import static io.harness.connector.jacksontests.ConnectorJacksonTestHelper.readFileAsString;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerRegistryProviderType;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class ConnectorDeserializerTest extends CategoryTest {
  private ObjectMapper objectMapper;
  String dockerRegistryUrl = "https://index.docker.io/v2/";

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
  }

  @Test
  @Owner(developers = OwnerRule.HARI)
  @Category(UnitTests.class)
  public void testDeserializationOfConnector() {
    String connectorInput =
        readFileAsString("440-connector-nextgen/src/test/resources/connector/connectorDeserializerTest.json");
    ConnectorDTO inputConnector = null;
    try {
      inputConnector = objectMapper.readValue(connectorInput, ConnectorDTO.class);
    } catch (Exception ex) {
      Assert.fail("Encountered exception while deserializing docker connector " + ex.getMessage());
    }
    DockerConnectorDTO dockerConnectorDTO = createDockerConfigWithAnonymousCreds();
    Map<String, String> tags = new HashMap<String, String>() {
      {
        put("company", "Harness");
        put("env", "dev");
      }
    };
    ConnectorInfoDTO connectorRequestDTO = ConnectorInfoDTO.builder()
                                               .name("name")
                                               .identifier("identifier")
                                               .description("description")
                                               .orgIdentifier("orgIdentifier")
                                               .tags(tags)
                                               .connectorType(ConnectorType.DOCKER)
                                               .connectorConfig(dockerConnectorDTO)
                                               .build();
    ConnectorDTO connectorDTO = ConnectorDTO.builder().connectorInfo(connectorRequestDTO).build();
    assertThat(inputConnector).isEqualTo(connectorDTO);
  }

  private DockerConnectorDTO createDockerConfigWithAnonymousCreds() {
    return DockerConnectorDTO.builder()
        .dockerRegistryUrl(dockerRegistryUrl)
        .providerType(DockerRegistryProviderType.DOCKER_HUB)
        .auth(DockerAuthenticationDTO.builder().authType(DockerAuthType.ANONYMOUS).build())
        .build();
  }
}
