package io.harness.cdng.artifact.resources.docker.mappers;

import static io.harness.rule.OwnerRule.SAHIL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.resources.docker.dtos.DockerBuildDetailsDTO;
import io.harness.cdng.artifact.resources.docker.dtos.DockerResponseDTO;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class DockerResourceMapperTest extends CategoryTest {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testToDockerResponse() {
    List<DockerArtifactDelegateResponse> dockerArtifactDelegateResponses =
        Lists.newArrayList(DockerArtifactDelegateResponse.builder()
                               .buildDetails(ArtifactBuildDetailsNG.builder().build())
                               .imagePath("/")
                               .build());
    DockerResponseDTO dockerResponseDTO = DockerResourceMapper.toDockerResponse(dockerArtifactDelegateResponses);
    assertThat(dockerResponseDTO).isNotNull();
    assertThat(dockerResponseDTO.getBuildDetailsList()).isNotEmpty();
    assertThat(dockerResponseDTO.getBuildDetailsList().get(0))
        .isEqualTo(DockerBuildDetailsDTO.builder().imagePath("/").build());
  }
}