package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.scm.ScmPushTaskParams;
import io.harness.delegate.task.scm.ScmPushTaskResponseData;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.time.Duration;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(DX)
public class ScmDelegateGitHelper implements ScmGitHelper {
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Override
  public ScmPushResponse pushToGitBasedOnChangeType(
      String yaml, ChangeType changeType, GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    ScmPushTaskParams scmPushTaskParams =
        ScmPushTaskParams.builder()
            .changeType(changeType)
            .scmConnector(infoForPush.getScmConnector())
            .gitFileDetails(ScmGitUtils.getGitFileDetails(gitBranchInfo, yaml).build())
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
    switch (changeType) {
      case ADD:
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
            scmPushTaskResponseData.getCreateFileResponse().getStatus(),
            scmPushTaskResponseData.getCreateFileResponse().getError());
        return ScmGitUtils.createScmCreateFileResponse(yaml, infoForPush);
      case MODIFY:
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
            scmPushTaskResponseData.getUpdateFileResponse().getStatus(),
            scmPushTaskResponseData.getUpdateFileResponse().getError());
        return ScmGitUtils.createScmUpdateFileResponse(yaml, infoForPush);
      case DELETE:
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
            scmPushTaskResponseData.getDeleteFileResponse().getStatus(),
            scmPushTaskResponseData.getDeleteFileResponse().getError());
        return ScmGitUtils.createScmDeleteFileResponse(yaml, infoForPush);
      case NONE:
      case RENAME:
        throw new NotImplementedException(changeType + " is not Implemented");
      default:
        throw new UnknownEnumTypeException("Change Type", changeType.toString());
    }
  }
}
