package io.harness.pms.expressions.utils;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGTaskType;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.engine.pms.tasks.NgDelegate2TaskExecutor;
import io.harness.exception.ArtifactServerException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.steps.StepUtils;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nonnull;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class EcrImagePullSecretHelper {
  @Named("PRIVILEGED") @Inject private SecretManagerClientService secretManagerClientService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject private NgDelegate2TaskExecutor ngDelegate2TaskExecutor;
  @Inject private KryoSerializer kryoSerializer;

  BaseNGAccess getBaseNGAccess(String accountId, String orgIdentifier, String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }

  List<EncryptedDataDetail> getEncryptionDetails(@Nonnull AwsConnectorDTO awsConnectorDTO, @Nonnull NGAccess ngAccess) {
    if (awsConnectorDTO.getCredential() != null && awsConnectorDTO.getCredential().getConfig() != null) {
      return secretManagerClientService.getEncryptionDetails(
          ngAccess, (AwsManualConfigSpecDTO) awsConnectorDTO.getCredential().getConfig());
    }
    return new ArrayList<>();
  }

  ArtifactTaskExecutionResponse executeSyncTask(Ambiance ambiance, EcrArtifactDelegateRequest ecrRequest,
      ArtifactTaskType taskType, BaseNGAccess ngAccess, String ifFailedMessage) {
    DelegateResponseData responseData = getResponseData(ambiance, ngAccess, ecrRequest, taskType);
    return getTaskExecutionResponse(responseData, ifFailedMessage);
  }

  private DelegateResponseData getResponseData(Ambiance ambiance, BaseNGAccess ngAccess,
      EcrArtifactDelegateRequest ecrRequest, ArtifactTaskType artifactTaskType) {
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .accountId(ngAccess.getAccountIdentifier())
                                                        .artifactTaskType(artifactTaskType)
                                                        .attributes(ecrRequest)
                                                        .build();
    TaskDetails taskDetails =
        TaskDetails.newBuilder()
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(artifactTaskParameters)))
            .setMode(TaskMode.SYNC)
            .setExecutionTimeout(Duration.newBuilder().setSeconds(20).build())
            .setType(TaskType.newBuilder().setType(NGTaskType.ECR_ARTIFACT_TASK_NG.name()).build())
            .build();

    TaskRequest taskRequest =
        StepUtils.prepareTaskRequest(ambiance, taskDetails, new ArrayList<>(), new ArrayList<>(), null);

    ResponseData response = ngDelegate2TaskExecutor.executeTask(new HashMap<String, String>(), taskRequest);
    return (DelegateResponseData) kryoSerializer.asInflatedObject(((BinaryResponseData) response).getData());
  }
  private ArtifactTaskExecutionResponse getTaskExecutionResponse(
      DelegateResponseData responseData, String ifFailedMessage) {
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new ArtifactServerException(ifFailedMessage + " - " + errorNotifyResponseData.getErrorMessage());
    }
    ArtifactTaskResponse artifactTaskResponse = (ArtifactTaskResponse) responseData;
    if (artifactTaskResponse.getCommandExecutionStatus() != SUCCESS) {
      throw new ArtifactServerException(ifFailedMessage + " - " + artifactTaskResponse.getErrorMessage()
          + " with error code: " + artifactTaskResponse.getErrorCode());
    }
    return artifactTaskResponse.getArtifactTaskExecutionResponse();
  }
}
