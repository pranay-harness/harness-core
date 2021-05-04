package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(HarnessTeam.DX)
public class ScmPushTask extends AbstractDelegateRunnableTask {
  @Inject ScmDelegateClient scmDelegateClient;
  @Inject ScmServiceClient scmServiceClient;

  public ScmPushTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmPushTaskParams scmPushTaskParams = (ScmPushTaskParams) parameters;
    final DecryptableEntity apiAccessDecryptableEntity =
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmPushTaskParams.getScmConnector());

    switch (scmPushTaskParams.getPushTaskType()) {
      case CREATE: {
        CreateFileResponse createFileResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.createFile(scmPushTaskParams.getScmConnector(), scmPushTaskParams.getGitFileDetails(),
                SCMGrpc.newBlockingStub(c)));
        return ScmPushTaskResponseData.builder()
            .createFileResponse(createFileResponse)
            .pushTaskType(PushTaskType.CREATE)
            .build();
      }
      case DELETE: {
        DeleteFileResponse deleteFileResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.deleteFile(scmPushTaskParams.getScmConnector(),
                GitFilePathDetails.builder()
                    .branch(scmPushTaskParams.getGitFileDetails().getBranch())
                    .filePath(scmPushTaskParams.getGitFileDetails().getFilePath())
                    .build(),
                SCMGrpc.newBlockingStub(c)));
        return ScmPushTaskResponseData.builder()
            .deleteFileResponse(deleteFileResponse)
            .pushTaskType(PushTaskType.DELETE)
            .build();
      }
      case UPDATE: {
        UpdateFileResponse updateFileResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.updateFile(scmPushTaskParams.getScmConnector(), scmPushTaskParams.getGitFileDetails(),
                SCMGrpc.newBlockingStub(c)));
        return ScmPushTaskResponseData.builder()
            .updateFileResponse(updateFileResponse)
            .pushTaskType(PushTaskType.UPDATE)
            .build();
      }
      default: {
        throw new NotImplementedException("Not Implemented");
      }
    }
  }
}
