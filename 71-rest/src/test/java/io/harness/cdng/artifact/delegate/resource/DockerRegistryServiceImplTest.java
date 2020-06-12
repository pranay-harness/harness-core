package io.harness.cdng.artifact.delegate.resource;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.rule.Owner;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.exception.InvalidArtifactServerException;

import java.io.IOException;

public class DockerRegistryServiceImplTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public WireMockRule wireMockRule = new WireMockRule(9552);
  @Mock private DockerPublicRegistryProcessor dockerPublicRegistryProcessor;
  @Inject @InjectMocks private DockerRegistryServiceImpl dockerRegistryService;

  private static DockerhubConnectorConfig connectorConfig;

  @BeforeClass
  public static void setUp() {
    String url = "http://localhost:9552/";
    connectorConfig = DockerhubConnectorConfig.builder().registryUrl(url).identifier("CONNECTOR_CONFIG").build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulBuild() throws IOException {
    DockerArtifactAttributes dockerArtifactAttributes =
        DockerArtifactAttributes.builder().imagePath("imagePath").tag("tag").dockerHubConnector("connector").build();
    doReturn(dockerArtifactAttributes)
        .when(dockerPublicRegistryProcessor)
        .getLastSuccessfulBuild(connectorConfig, "imagePath", "tag");
    ArtifactAttributes lastSuccessfulBuild =
        dockerRegistryService.getLastSuccessfulBuild(connectorConfig, "imagePath", "tag");

    verify(dockerPublicRegistryProcessor).getLastSuccessfulBuild(connectorConfig, "imagePath", "tag");
    assertThat(lastSuccessfulBuild).isInstanceOf(DockerArtifactAttributes.class);
    DockerArtifactAttributes attributes = (DockerArtifactAttributes) lastSuccessfulBuild;
    assertThat(dockerArtifactAttributes.getImagePath()).isEqualTo(attributes.getImagePath());
    assertThat(dockerArtifactAttributes.getTag()).isEqualTo(attributes.getTag());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLatestSuccessfulBuildWithException() throws IOException {
    doThrow(new InvalidArtifactServerException("Mock Exception"))
        .when(dockerPublicRegistryProcessor)
        .getLastSuccessfulBuild(any(), any(), any());
    assertThatThrownBy(() -> dockerRegistryService.getLastSuccessfulBuild(connectorConfig, "image", "tag"))
        .isInstanceOf(ArtifactServerException.class);
  }
}