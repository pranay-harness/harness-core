/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts.docker;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DockerArtifactTaskHelperTest extends CategoryTest {
  @Mock private DockerArtifactTaskHandler dockerArtifactTaskHandler;
  @Mock private SecretDecryptionService secretDecryptionService;

  @InjectMocks private DockerArtifactTaskHelper dockerArtifactTaskHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetArtifactCollectResponseGetLastSuccessfulBuild() {
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    DockerArtifactDelegateRequest dockerArtifactDelegateRequest =
        DockerArtifactDelegateRequest.builder()
            .dockerConnectorDTO(DockerConnectorDTO.builder().auth(DockerAuthenticationDTO.builder().build()).build())
            .build();
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .attributes(dockerArtifactDelegateRequest)
                                                        .artifactTaskType(ArtifactTaskType.GET_LAST_SUCCESSFUL_BUILD)
                                                        .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse = ArtifactTaskExecutionResponse.builder().build();
    when(dockerArtifactTaskHandler.getLastSuccessfulBuild(dockerArtifactDelegateRequest))
        .thenReturn(artifactTaskExecutionResponse);

    ArtifactTaskResponse artifactTaskResponse =
        dockerArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
    assertThat(artifactTaskResponse).isNotNull();
    assertThat(artifactTaskResponse.getArtifactTaskExecutionResponse()).isEqualTo(artifactTaskExecutionResponse);

    verify(secretDecryptionService).decrypt(any(), any());
    verify(dockerArtifactTaskHandler).getLastSuccessfulBuild(dockerArtifactDelegateRequest);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetArtifactCollectResponseGetBuilds() {
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    DockerArtifactDelegateRequest dockerArtifactDelegateRequest =
        DockerArtifactDelegateRequest.builder()
            .dockerConnectorDTO(DockerConnectorDTO.builder().auth(DockerAuthenticationDTO.builder().build()).build())
            .build();
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .attributes(dockerArtifactDelegateRequest)
                                                        .artifactTaskType(ArtifactTaskType.GET_BUILDS)
                                                        .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse = ArtifactTaskExecutionResponse.builder().build();
    when(dockerArtifactTaskHandler.getBuilds(dockerArtifactDelegateRequest)).thenReturn(artifactTaskExecutionResponse);

    ArtifactTaskResponse artifactTaskResponse =
        dockerArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
    assertThat(artifactTaskResponse).isNotNull();
    assertThat(artifactTaskResponse.getArtifactTaskExecutionResponse()).isEqualTo(artifactTaskExecutionResponse);

    verify(secretDecryptionService).decrypt(any(), any());
    verify(dockerArtifactTaskHandler).getBuilds(dockerArtifactDelegateRequest);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetArtifactCollectResponseGetLabels() {
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    DockerArtifactDelegateRequest dockerArtifactDelegateRequest =
        DockerArtifactDelegateRequest.builder()
            .dockerConnectorDTO(DockerConnectorDTO.builder().auth(DockerAuthenticationDTO.builder().build()).build())
            .build();
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .attributes(dockerArtifactDelegateRequest)
                                                        .artifactTaskType(ArtifactTaskType.GET_LABELS)
                                                        .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse = ArtifactTaskExecutionResponse.builder().build();
    when(dockerArtifactTaskHandler.getLabels(dockerArtifactDelegateRequest)).thenReturn(artifactTaskExecutionResponse);

    ArtifactTaskResponse artifactTaskResponse =
        dockerArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
    assertThat(artifactTaskResponse).isNotNull();
    assertThat(artifactTaskResponse.getArtifactTaskExecutionResponse()).isEqualTo(artifactTaskExecutionResponse);

    verify(secretDecryptionService).decrypt(any(), any());
    verify(dockerArtifactTaskHandler).getLabels(dockerArtifactDelegateRequest);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetArtifactCollectResponseValidateArtifactServers() {
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    DockerArtifactDelegateRequest dockerArtifactDelegateRequest =
        DockerArtifactDelegateRequest.builder()
            .dockerConnectorDTO(DockerConnectorDTO.builder().auth(DockerAuthenticationDTO.builder().build()).build())
            .build();
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .attributes(dockerArtifactDelegateRequest)
                                                        .artifactTaskType(ArtifactTaskType.VALIDATE_ARTIFACT_SERVER)
                                                        .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse = ArtifactTaskExecutionResponse.builder().build();
    when(dockerArtifactTaskHandler.validateArtifactServer(dockerArtifactDelegateRequest))
        .thenReturn(artifactTaskExecutionResponse);

    ArtifactTaskResponse artifactTaskResponse =
        dockerArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
    assertThat(artifactTaskResponse).isNotNull();
    assertThat(artifactTaskResponse.getArtifactTaskExecutionResponse()).isEqualTo(artifactTaskExecutionResponse);

    verify(secretDecryptionService).decrypt(any(), any());
    verify(dockerArtifactTaskHandler).validateArtifactServer(dockerArtifactDelegateRequest);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetArtifactCollectResponseValidateArtifactSource() {
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    DockerArtifactDelegateRequest dockerArtifactDelegateRequest =
        DockerArtifactDelegateRequest.builder()
            .dockerConnectorDTO(DockerConnectorDTO.builder()
                                    .dockerRegistryUrl("")
                                    .auth(DockerAuthenticationDTO.builder().build())
                                    .build())
            .build();
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .attributes(dockerArtifactDelegateRequest)
                                                        .artifactTaskType(ArtifactTaskType.VALIDATE_ARTIFACT_SOURCE)
                                                        .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse = ArtifactTaskExecutionResponse.builder().build();
    when(dockerArtifactTaskHandler.validateArtifactImage(dockerArtifactDelegateRequest))
        .thenReturn(artifactTaskExecutionResponse);

    ArtifactTaskResponse artifactTaskResponse =
        dockerArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
    assertThat(artifactTaskResponse).isNotNull();
    assertThat(artifactTaskResponse.getArtifactTaskExecutionResponse()).isEqualTo(artifactTaskExecutionResponse);

    verify(secretDecryptionService).decrypt(any(), any());
    verify(dockerArtifactTaskHandler).validateArtifactImage(dockerArtifactDelegateRequest);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetArtifactCollectResponsegetFeeds() {
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    DockerArtifactDelegateRequest dockerArtifactDelegateRequest =
        DockerArtifactDelegateRequest.builder()
            .dockerConnectorDTO(DockerConnectorDTO.builder().auth(DockerAuthenticationDTO.builder().build()).build())
            .build();
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .attributes(dockerArtifactDelegateRequest)
                                                        .artifactTaskType(ArtifactTaskType.GET_FEEDS)
                                                        .build();

    ArtifactTaskResponse artifactTaskResponse =
        dockerArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
    assertThat(artifactTaskResponse).isNotNull();
    assertThat(artifactTaskResponse.getArtifactTaskExecutionResponse()).isNull();
    assertThat(artifactTaskResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);

    verify(secretDecryptionService).decrypt(any(), any());
  }
}
