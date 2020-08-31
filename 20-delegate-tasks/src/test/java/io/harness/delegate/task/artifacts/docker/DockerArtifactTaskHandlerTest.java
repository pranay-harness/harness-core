package io.harness.delegate.task.artifacts.docker;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.artifacts.docker.service.DockerRegistryService;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class DockerArtifactTaskHandlerTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock DockerRegistryService dockerRegistryService;
  @InjectMocks DockerArtifactTaskHandler dockerArtifactService;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulBuild() {
    BuildDetailsInternal buildDetailsInternal = BuildDetailsInternal.builder().number("tag").build();
    DockerInternalConfig dockerInternalConfig = DockerInternalConfig.builder().dockerRegistryUrl("URL").build();
    doReturn(buildDetailsInternal)
        .when(dockerRegistryService)
        .verifyBuildNumber(dockerInternalConfig, "imagePath", "tag");
    DockerArtifactDelegateRequest sourceAttributes =
        DockerArtifactDelegateRequest.builder()
            .imagePath("imagePath")
            .tag("tag")
            .dockerConnectorDTO(DockerConnectorDTO.builder()
                                    .url("URL")
                                    .authScheme(DockerAuthenticationDTO.builder()
                                                    .credentials(DockerUserNamePasswordDTO.builder()
                                                                     .passwordRef(SecretRefData.builder().build())
                                                                     .build())
                                                    .build())
                                    .build())
            .build();
    ArtifactTaskExecutionResponse lastSuccessfulBuild = dockerArtifactService.getLastSuccessfulBuild(sourceAttributes);
    assertThat(lastSuccessfulBuild).isNotNull();
    assertThat(lastSuccessfulBuild.getArtifactDelegateResponses().size()).isEqualTo(1);
    assertThat(lastSuccessfulBuild.getArtifactDelegateResponses().get(0))
        .isInstanceOf(DockerArtifactDelegateResponse.class);
    DockerArtifactDelegateResponse attributes =
        (DockerArtifactDelegateResponse) lastSuccessfulBuild.getArtifactDelegateResponses().get(0);
    assertThat(attributes.getImagePath()).isEqualTo(sourceAttributes.getImagePath());
    assertThat(attributes.getTag()).isEqualTo(sourceAttributes.getTag());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetLastSuccessfulBuildFromRegex() {
    BuildDetailsInternal buildDetailsInternal = BuildDetailsInternal.builder().number("tag").build();
    DockerInternalConfig dockerInternalConfig = DockerInternalConfig.builder().dockerRegistryUrl("URL").build();
    doReturn(buildDetailsInternal)
        .when(dockerRegistryService)
        .getLastSuccessfulBuildFromRegex(dockerInternalConfig, "imagePath", "tagRegex");
    DockerArtifactDelegateRequest sourceAttributes =
        DockerArtifactDelegateRequest.builder()
            .imagePath("imagePath")
            .tagRegex("tagRegex")
            .dockerConnectorDTO(DockerConnectorDTO.builder()
                                    .url("URL")
                                    .authScheme(DockerAuthenticationDTO.builder()
                                                    .credentials(DockerUserNamePasswordDTO.builder()
                                                                     .passwordRef(SecretRefData.builder().build())
                                                                     .build())
                                                    .build())
                                    .build())
            .build();
    ArtifactTaskExecutionResponse lastSuccessfulBuild = dockerArtifactService.getLastSuccessfulBuild(sourceAttributes);
    assertThat(lastSuccessfulBuild).isNotNull();
    assertThat(lastSuccessfulBuild.getArtifactDelegateResponses().size()).isEqualTo(1);
    assertThat(lastSuccessfulBuild.getArtifactDelegateResponses().get(0))
        .isInstanceOf(DockerArtifactDelegateResponse.class);
    DockerArtifactDelegateResponse attributes =
        (DockerArtifactDelegateResponse) lastSuccessfulBuild.getArtifactDelegateResponses().get(0);
    assertThat(attributes.getImagePath()).isEqualTo(sourceAttributes.getImagePath());
    assertThat(attributes.getTag()).isEqualTo("tag");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testIsRegex() {
    DockerArtifactDelegateRequest sourceAttributes =
        DockerArtifactDelegateRequest.builder().imagePath("imagePath").tag("tag").tagRegex("tagRegex").build();
    boolean regex = dockerArtifactService.isRegex(sourceAttributes);
    assertThat(regex).isTrue();
    sourceAttributes = DockerArtifactDelegateRequest.builder().imagePath("imagePath").tagRegex("tagRegex").build();
    regex = dockerArtifactService.isRegex(sourceAttributes);
    assertThat(regex).isTrue();
    sourceAttributes = DockerArtifactDelegateRequest.builder().imagePath("imagePath").tag("tag").build();
    regex = dockerArtifactService.isRegex(sourceAttributes);
    assertThat(regex).isFalse();
  }
}