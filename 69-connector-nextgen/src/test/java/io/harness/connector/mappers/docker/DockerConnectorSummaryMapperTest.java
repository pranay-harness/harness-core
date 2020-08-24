package io.harness.connector.mappers.docker;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.docker.DockerConnectorSummaryDTO;
import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class DockerConnectorSummaryMapperTest extends CategoryTest {
  @InjectMocks DockerConnectorSummaryMapper dockerConnectorSummaryMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void toConnectorConfigSummaryDTOTest() {
    String dockerRegistryUrl = "url";
    DockerConnector dockerConnector = DockerConnector.builder().url("url").build();
    DockerConnectorSummaryDTO dockerConnectorSummaryDTO =
        dockerConnectorSummaryMapper.toConnectorConfigSummaryDTO(dockerConnector);
    assertThat(dockerConnectorSummaryDTO.getDockerRegistryUrl()).isEqualTo(dockerRegistryUrl);
  }
}