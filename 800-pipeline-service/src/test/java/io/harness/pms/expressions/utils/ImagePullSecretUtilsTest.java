/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.expressions.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngpipeline.artifact.bean.ArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.EcrArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.GcrArtifactOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@OwnedBy(PIPELINE)
@PrepareForTest({SafeHttpCall.class})
public class ImagePullSecretUtilsTest extends CategoryTest {
  @Mock private EcrImagePullSecretHelper ecrImagePullSecretHelper;
  @Mock private ConnectorResourceClient connectorResourceClient;
  @InjectMocks private ImagePullSecretUtils imagePullSecretUtils;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testEcrImagePullSecret() throws IOException {
    final String ecrUrl = "https://aws_account_id.dkr.ecr.region.amazonaws.com/imageName";
    final String authToken = "QVdTOnJhbmRvbXRva2VuCg==";
    final String ecrImagePullSecret =
        "${imageSecret.create(\"https://https://aws_account_id.dkr.ecr.region.amazonaws.com/\", \"AWS\", \"randomtoken\n"
        + "\")}";
    PowerMockito.mockStatic(SafeHttpCall.class);
    ArtifactOutcome artifactOutcome =
        EcrArtifactOutcome.builder().type(ArtifactSourceConstants.ECR_NAME).connectorRef("account").build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .putSetupAbstractions("accountId", "accountId")
                            .putSetupAbstractions("projectIdentifier", "projectId")
                            .putSetupAbstractions("orgIdentifier", "orgIdentifier")
                            .build();
    ArtifactTaskExecutionResponse responseForImageUrl =
        ArtifactTaskExecutionResponse.builder()
            .artifactDelegateResponse(EcrArtifactDelegateResponse.builder().imageUrl(ecrUrl).build())
            .build();
    ArtifactTaskExecutionResponse responseForAuthToken =
        ArtifactTaskExecutionResponse.builder()
            .artifactDelegateResponse(EcrArtifactDelegateResponse.builder().authToken(authToken).build())
            .build();
    BaseNGAccess baseNGAccess = BaseNGAccess.builder().build();
    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>();
    encryptionDetails.add(EncryptedDataDetail.builder().build());
    ResponseDTO<Optional<ConnectorDTO>> responseDTO = ResponseDTO.newResponse(
        Optional.of(ConnectorDTO.builder().connectorInfo(ConnectorInfoDTO.builder().build()).build()));

    when(connectorResourceClient.get(any(), any(), any(), any())).thenReturn(null);
    when(SafeHttpCall.execute(any())).thenReturn(responseDTO);
    when(ecrImagePullSecretHelper.getBaseNGAccess(any(), any(), any())).thenReturn(baseNGAccess);
    when(ecrImagePullSecretHelper.getEncryptionDetails(any(), any())).thenReturn(encryptionDetails);
    when(ecrImagePullSecretHelper.executeSyncTask(any(), any(), eq(ArtifactTaskType.GET_IMAGE_URL), any(), any()))
        .thenReturn(responseForImageUrl);
    when(ecrImagePullSecretHelper.executeSyncTask(any(), any(), eq(ArtifactTaskType.GET_AUTH_TOKEN), any(), any()))
        .thenReturn(responseForAuthToken);
    assertThat(imagePullSecretUtils.getImagePullSecret(artifactOutcome, ambiance).equals(ecrImagePullSecret));
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGcrImagePullSecret() throws IOException {
    ArtifactOutcome artifactOutcome = GcrArtifactOutcome.builder()
                                          .registryHostname("us.gcr.io")
                                          .imagePath("test-image")
                                          .type(ArtifactSourceConstants.GCR_NAME)
                                          .connectorRef("account")
                                          .build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .putSetupAbstractions("accountId", "accountId")
                            .putSetupAbstractions("projectIdentifier", "projectId")
                            .putSetupAbstractions("orgIdentifier", "orgIdentifier")
                            .build();

    GcpConnectorCredentialDTO connectorCredentialDTO =
        GcpConnectorCredentialDTO.builder()
            .config(GcpManualDetailsDTO.builder()
                        .secretKeyRef(SecretRefData.builder().identifier("secret").build())
                        .build())
            .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
            .build();
    ResponseDTO<Optional<ConnectorDTO>> responseDTO = ResponseDTO.newResponse(Optional.of(
        ConnectorDTO.builder()
            .connectorInfo(ConnectorInfoDTO.builder()
                               .connectorConfig(GcpConnectorDTO.builder().credential(connectorCredentialDTO).build())
                               .build())
            .build()));

    when(connectorResourceClient.get(any(), any(), any(), any())).thenReturn(null);
    assertThatThrownBy(() -> imagePullSecretUtils.getImagePullSecret(artifactOutcome, ambiance))
        .isInstanceOf(InvalidRequestException.class);

    PowerMockito.mockStatic(SafeHttpCall.class);
    when(SafeHttpCall.execute(any())).thenReturn(responseDTO);
    assertEquals(imagePullSecretUtils.getImagePullSecret(artifactOutcome, ambiance),
        "${imageSecret.create(\"us.gcr.io/test-image\", \"_json_key\", ${ngSecretManager.obtain(\"null\", 0)})}");
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testDockerImagePullSecret() throws IOException {
    ArtifactOutcome artifactOutcome = DockerArtifactOutcome.builder()
                                          .imagePath("test-image")
                                          .type(ArtifactSourceConstants.DOCKER_REGISTRY_NAME)
                                          .connectorRef("account")
                                          .build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .putSetupAbstractions("accountId", "accountId")
                            .putSetupAbstractions("projectIdentifier", "projectId")
                            .putSetupAbstractions("orgIdentifier", "orgIdentifier")
                            .build();

    DockerAuthenticationDTO authenticationDTO =
        DockerAuthenticationDTO.builder()
            .authType(DockerAuthType.USER_PASSWORD)
            .credentials(DockerUserNamePasswordDTO.builder()
                             .usernameRef(SecretRefData.builder().identifier("username").build())
                             .passwordRef(SecretRefData.builder().identifier("password").build())
                             .build())
            .build();
    ResponseDTO<Optional<ConnectorDTO>> responseDTO = ResponseDTO.newResponse(
        Optional.of(ConnectorDTO.builder()
                        .connectorInfo(ConnectorInfoDTO.builder()
                                           .connectorConfig(DockerConnectorDTO.builder()
                                                                .dockerRegistryUrl("index.docker.io")
                                                                .auth(authenticationDTO)
                                                                .build())
                                           .build())
                        .build()));

    PowerMockito.mockStatic(SafeHttpCall.class);
    when(SafeHttpCall.execute(any())).thenReturn(responseDTO);
    assertEquals(imagePullSecretUtils.getImagePullSecret(artifactOutcome, ambiance),
        "${imageSecret.create(\"index.docker.io\", ${ngSecretManager.obtain(\"null\", 0)}, ${ngSecretManager.obtain(\"null\", 0)})}");
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testUnsupportedArtifactSourceType() {
    ArtifactOutcome artifactOutcome =
        DockerArtifactOutcome.builder().imagePath("test-image").type("randomString").connectorRef("account").build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .putSetupAbstractions("accountId", "accountId")
                            .putSetupAbstractions("projectIdentifier", "projectId")
                            .putSetupAbstractions("orgIdentifier", "orgIdentifier")
                            .build();

    assertThatThrownBy(() -> imagePullSecretUtils.getImagePullSecret(artifactOutcome, ambiance))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
