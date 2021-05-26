package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.beans.gitsync.GitFileDetails.GitFileDetailsBuilder;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.scm.ScmPushTaskParams;
import io.harness.delegate.task.scm.ScmPushTaskResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ScmException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@OwnedBy(DX)
public class ScmDelegateGitHelper implements ScmGitHelper {
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Override
  public ScmPushResponse pushToGitBasedOnChangeType(
      String yaml, ChangeType changeType, GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    GitFileDetailsBuilder gitFileDetails = ScmGitUtils.getGitFileDetails(gitBranchInfo, yaml);
    if (changeType.equals(ChangeType.MODIFY)) {
      gitFileDetails.oldFileSha(gitBranchInfo.getLastObjectId());
    }
    ScmPushTaskParams scmPushTaskParams = ScmPushTaskParams.builder()
                                              .changeType(changeType)
                                              .scmConnector(infoForPush.getScmConnector())
                                              .gitFileDetails(gitFileDetails.build())
                                              .encryptedDataDetails(infoForPush.getEncryptedDataDetailList())
                                              .build();
    DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                  .accountId(infoForPush.getAccountId())
                                                  .taskType(TaskType.SCM_PUSH_TASK.name())
                                                  .taskParameters(scmPushTaskParams)
                                                  .executionTimeout(Duration.ofMinutes(2))
                                                  .build();
    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    ScmPushTaskResponseData scmPushTaskResponseData = (ScmPushTaskResponseData) responseData;
    ScmPushResponse scmPushResponse = null;
    try {
      switch (changeType) {
        case ADD:
          try {
            ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
                CreateFileResponse.parseFrom(scmPushTaskResponseData.getCreateFileResponse()).getStatus(),
                CreateFileResponse.parseFrom(scmPushTaskResponseData.getCreateFileResponse()).getError());
          } catch (ScmException e) {
            if (ErrorCode.SCM_CONFLICT_ERROR.equals(e.getCode())) {
              throw new InvalidRequestException(String.format(
                  "A file with name %s already exists in the remote Git repository", gitBranchInfo.getFilePath()));
            }
            throw e;
          }
          return ScmGitUtils.createScmCreateFileResponse(yaml, infoForPush);
        case MODIFY:
          ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
              UpdateFileResponse.parseFrom(scmPushTaskResponseData.getUpdateFileResponse()).getStatus(),
              UpdateFileResponse.parseFrom(scmPushTaskResponseData.getUpdateFileResponse()).getError());
          return ScmGitUtils.createScmUpdateFileResponse(yaml, infoForPush);
        case DELETE:
          ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
              DeleteFileResponse.parseFrom(scmPushTaskResponseData.getDeleteFileResponse()).getStatus(),
              DeleteFileResponse.parseFrom(scmPushTaskResponseData.getDeleteFileResponse()).getError());
          return ScmGitUtils.createScmDeleteFileResponse(yaml, infoForPush);
        case NONE:
        case RENAME:
          throw new NotImplementedException(changeType + " is not Implemented");
        default:
          throw new UnknownEnumTypeException("Change Type", changeType.toString());
      }
    } catch (InvalidProtocolBufferException e) {
      throw new UnexpectedException("Unexpected error occurred while doing scm operation");
    }
  }
}
