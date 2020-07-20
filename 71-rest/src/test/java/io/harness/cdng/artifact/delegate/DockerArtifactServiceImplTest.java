package io.harness.cdng.artifact.delegate;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.delegate.beans.ArtifactAttributes;
import io.harness.cdng.artifact.delegate.beans.DockerArtifactAttributes;
import io.harness.cdng.artifact.delegate.beans.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.delegate.beans.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.resource.DockerRegistryService;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.utils.WingsTestConstants;

public class DockerArtifactServiceImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock DockerRegistryService dockerRegistryService;
  @Inject @InjectMocks DockerArtifactServiceImpl dockerArtifactService;

  static final String url = "http://localhost:9552/";
  private static final DockerhubConnectorConfig connectorConfig =
      DockerhubConnectorConfig.builder().registryUrl(url).identifier("CONNECTOR_CONFIG").build();

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulBuild() {
    DockerArtifactAttributes dockerArtifactAttributes =
        DockerArtifactAttributes.builder().imagePath("imagePath").tag("tag").dockerHubConnector("connector").build();
    DockerArtifactSourceAttributes sourceAttributes = DockerArtifactSourceAttributes.builder()
                                                          .dockerhubConnector("connector")
                                                          .imagePath("imagePath")
                                                          .tag("tag")
                                                          .build();
    doReturn(dockerArtifactAttributes)
        .when(dockerRegistryService)
        .getLastSuccessfulBuild(connectorConfig, "imagePath", "tag");
    ArtifactAttributes lastSuccessfulBuild =
        dockerArtifactService.getLastSuccessfulBuild(WingsTestConstants.APP_ID, sourceAttributes, connectorConfig);
    assertThat(lastSuccessfulBuild).isInstanceOf(DockerArtifactAttributes.class);
    DockerArtifactAttributes attributes = (DockerArtifactAttributes) lastSuccessfulBuild;
    verify(dockerRegistryService).getLastSuccessfulBuild(connectorConfig, "imagePath", "tag");
    assertThat(attributes.getImagePath()).isEqualTo(sourceAttributes.getImagePath());
    assertThat(attributes.getTag()).isEqualTo(sourceAttributes.getTag());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulBuildFromRegex() {
    DockerArtifactAttributes dockerArtifactAttributes = DockerArtifactAttributes.builder()
                                                            .imagePath("imagePath")
                                                            .tag("tagRegexNew")
                                                            .dockerHubConnector("connector")
                                                            .build();
    DockerArtifactSourceAttributes sourceAttributes = DockerArtifactSourceAttributes.builder()
                                                          .dockerhubConnector("connector")
                                                          .imagePath("imagePath")
                                                          .tagRegex("tagRegex")
                                                          .build();
    doReturn(dockerArtifactAttributes)
        .when(dockerRegistryService)
        .getLastSuccessfulBuildFromRegex(connectorConfig, "imagePath", "tagRegex");
    ArtifactAttributes lastSuccessfulBuild =
        dockerArtifactService.getLastSuccessfulBuild(WingsTestConstants.APP_ID, sourceAttributes, connectorConfig);
    assertThat(lastSuccessfulBuild).isInstanceOf(DockerArtifactAttributes.class);
    DockerArtifactAttributes attributes = (DockerArtifactAttributes) lastSuccessfulBuild;
    verify(dockerRegistryService).getLastSuccessfulBuildFromRegex(connectorConfig, "imagePath", "tagRegex");
    assertThat(attributes.getImagePath()).isEqualTo(sourceAttributes.getImagePath());
    assertThat(attributes.getTag()).isEqualTo(dockerArtifactAttributes.getTag());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testIsRegex() {
    DockerArtifactSourceAttributes sourceAttributes = DockerArtifactSourceAttributes.builder()
                                                          .dockerhubConnector("connector")
                                                          .imagePath("imagePath")
                                                          .tag("tag")
                                                          .tagRegex("tagRegex")
                                                          .build();
    boolean regex = dockerArtifactService.isRegex(sourceAttributes);
    assertThat(regex).isTrue();
    sourceAttributes = DockerArtifactSourceAttributes.builder()
                           .dockerhubConnector("connector")
                           .imagePath("imagePath")
                           .tagRegex("tagRegex")
                           .build();
    regex = dockerArtifactService.isRegex(sourceAttributes);
    assertThat(regex).isTrue();
    sourceAttributes = DockerArtifactSourceAttributes.builder()
                           .dockerhubConnector("connector")
                           .imagePath("imagePath")
                           .tag("tag")
                           .build();
    regex = dockerArtifactService.isRegex(sourceAttributes);
    assertThat(regex).isFalse();
  }
}