/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.artifact.resources.docker.mappers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.resources.docker.dtos.DockerBuildDetailsDTO;
import io.harness.cdng.artifact.resources.docker.dtos.DockerResponseDTO;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
