/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts.ecr;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.PIPELINE)
public class EcrArtifactTaskHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private EcrArtifactTaskHandler ecrArtifactTaskHandler;
  @Mock private SecretDecryptionService secretDecryptionService;

  @InjectMocks EcrArtifactTaskHelper ecrArtifactTaskHelper;
  private AwsConnectorDTO awsConnectorDTO =
      AwsConnectorDTO.builder()
          .credential(
              AwsCredentialDTO.builder()
                  .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                  .config(AwsManualConfigSpecDTO.builder()
                              .accessKey("access-key")
                              .secretKeyRef(SecretRefData.builder().decryptedValue("secret".toCharArray()).build())
                              .build())
                  .build())
          .build();

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldHandleGetImagesRequestSuccess() {
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse =
        ArtifactTaskExecutionResponse.builder().artifactImages(Arrays.asList("nginx", "todolist")).build();
    EcrArtifactDelegateRequest attributes =
        EcrArtifactDelegateRequest.builder().awsConnectorDTO(awsConnectorDTO).build();
    ArtifactTaskParameters artifactTaskParameters =
        ArtifactTaskParameters.builder().artifactTaskType(ArtifactTaskType.GET_IMAGES).attributes(attributes).build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    doReturn(artifactTaskExecutionResponse).when(ecrArtifactTaskHandler).getImages(eq(attributes));

    ArtifactTaskResponse response = ecrArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getArtifactTaskExecutionResponse()).isEqualTo(artifactTaskExecutionResponse);

    verify(secretDecryptionService, times(1)).decrypt(any(), any());
    verify(ecrArtifactTaskHandler, times(1)).getImages(eq(attributes));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldHandleGetImagesRequestFailure() {
    EcrArtifactDelegateRequest attributes =
        EcrArtifactDelegateRequest.builder().awsConnectorDTO(awsConnectorDTO).build();
    ArtifactTaskParameters artifactTaskParameters =
        ArtifactTaskParameters.builder().artifactTaskType(ArtifactTaskType.GET_IMAGES).attributes(attributes).build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    doThrow(new InvalidRequestException("Region not specified")).when(ecrArtifactTaskHandler).getImages(eq(attributes));

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> ecrArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters))
        .withMessageContaining("Region not specified");

    verify(secretDecryptionService, times(1)).decrypt(any(), any());
    verify(ecrArtifactTaskHandler, times(1)).getImages(eq(attributes));
  }
}
