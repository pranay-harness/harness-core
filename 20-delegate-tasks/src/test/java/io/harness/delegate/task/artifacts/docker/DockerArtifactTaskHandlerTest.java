package io.harness.delegate.task.artifacts.docker;

import static io.harness.artifacts.docker.service.DockerRegistryService.MAX_NO_OF_TAGS_PER_IMAGE;
import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.SAHIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.fabric8.utils.Lists;
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

import java.util.HashMap;

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
                                    .dockerRegistryUrl("URL")
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
                                    .dockerRegistryUrl("URL")
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
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetBuilds() {
    BuildDetailsInternal buildDetailsInternal = BuildDetailsInternal.builder().number("tag").build();
    DockerInternalConfig dockerInternalConfig = DockerInternalConfig.builder().dockerRegistryUrl("URL").build();
    doReturn(Lists.newArrayList(buildDetailsInternal))
        .when(dockerRegistryService)
        .getBuilds(dockerInternalConfig, "imagePath", MAX_NO_OF_TAGS_PER_IMAGE);
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
    ArtifactTaskExecutionResponse lastSuccessfulBuild = dockerArtifactService.getBuilds(sourceAttributes);
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
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetLabels() {
    BuildDetailsInternal buildDetailsInternal = BuildDetailsInternal.builder().number("tag").build();
    DockerInternalConfig dockerInternalConfig = DockerInternalConfig.builder().dockerRegistryUrl("URL").build();
    doReturn(Lists.newArrayList(new HashMap()))
        .when(dockerRegistryService)
        .getLabels(dockerInternalConfig, "imagePath", Lists.newArrayList("tag1"));
    DockerArtifactDelegateRequest sourceAttributes =
        DockerArtifactDelegateRequest.builder()
            .imagePath("imagePath")
            .tagsList(Lists.newArrayList("tag1"))
            .dockerConnectorDTO(DockerConnectorDTO.builder()
                                    .url("URL")
                                    .authScheme(DockerAuthenticationDTO.builder()
                                                    .credentials(DockerUserNamePasswordDTO.builder()
                                                                     .passwordRef(SecretRefData.builder().build())
                                                                     .build())
                                                    .build())
                                    .build())
            .build();
    ArtifactTaskExecutionResponse lastSuccessfulBuild = dockerArtifactService.getLabels(sourceAttributes);
    assertThat(lastSuccessfulBuild).isNotNull();
    assertThat(lastSuccessfulBuild.getArtifactDelegateResponses().size()).isEqualTo(1);
    assertThat(lastSuccessfulBuild.getArtifactDelegateResponses().get(0))
        .isInstanceOf(DockerArtifactDelegateResponse.class);
    DockerArtifactDelegateResponse attributes =
        (DockerArtifactDelegateResponse) lastSuccessfulBuild.getArtifactDelegateResponses().get(0);
    assertThat(attributes.getImagePath()).isEqualTo(sourceAttributes.getImagePath());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testValidateArtifactServer() {
    BuildDetailsInternal buildDetailsInternal = BuildDetailsInternal.builder().number("tag").build();
    DockerInternalConfig dockerInternalConfig = DockerInternalConfig.builder().dockerRegistryUrl("URL").build();
    doReturn(true).when(dockerRegistryService).validateCredentials(dockerInternalConfig);
    DockerArtifactDelegateRequest sourceAttributes =
        DockerArtifactDelegateRequest.builder()
            .imagePath("imagePath")
            .tagsList(Lists.newArrayList("tag1"))
            .dockerConnectorDTO(DockerConnectorDTO.builder()
                                    .url("URL")
                                    .authScheme(DockerAuthenticationDTO.builder()
                                                    .credentials(DockerUserNamePasswordDTO.builder()
                                                                     .passwordRef(SecretRefData.builder().build())
                                                                     .build())
                                                    .build())
                                    .build())
            .build();
    ArtifactTaskExecutionResponse lastSuccessfulBuild = dockerArtifactService.validateArtifactServer(sourceAttributes);
    assertThat(lastSuccessfulBuild).isNotNull();
    assertThat(lastSuccessfulBuild.isArtifactServerValid()).isTrue();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testValidateArtifactImage() {
    BuildDetailsInternal buildDetailsInternal = BuildDetailsInternal.builder().number("tag").build();
    DockerInternalConfig dockerInternalConfig = DockerInternalConfig.builder().dockerRegistryUrl("URL").build();
    doReturn(true).when(dockerRegistryService).verifyImageName(dockerInternalConfig, "imagePath");
    DockerArtifactDelegateRequest sourceAttributes =
        DockerArtifactDelegateRequest.builder()
            .imagePath("imagePath")
            .tagsList(Lists.newArrayList("tag1"))
            .dockerConnectorDTO(DockerConnectorDTO.builder()
                                    .url("URL")
                                    .authScheme(DockerAuthenticationDTO.builder()
                                                    .credentials(DockerUserNamePasswordDTO.builder()
                                                                     .passwordRef(SecretRefData.builder().build())
                                                                     .build())
                                                    .build())
                                    .build())
            .build();
    ArtifactTaskExecutionResponse lastSuccessfulBuild = dockerArtifactService.validateArtifactImage(sourceAttributes);
    assertThat(lastSuccessfulBuild).isNotNull();
    assertThat(lastSuccessfulBuild.isArtifactSourceValid()).isTrue();
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