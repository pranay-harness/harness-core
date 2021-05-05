package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(HarnessTeam.DX)
public class ScmGitFileTask extends AbstractDelegateRunnableTask {
  @Inject ScmDelegateClient scmDelegateClient;
  @Inject ScmServiceClient scmServiceClient;
  @Inject SecretDecryptionService secretDecryptionService;

  public ScmGitFileTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmGitFileTaskParams scmGitFileTaskParams = (ScmGitFileTaskParams) parameters;
    secretDecryptionService.decrypt(
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmGitFileTaskParams.getScmConnector()),
        scmGitFileTaskParams.getEncryptedDataDetails());
    switch (scmGitFileTaskParams.getGitFileTaskType()) {
      case LIST_FILES:
        final FileBatchContentResponse fileBatchContentResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.listFiles(scmGitFileTaskParams.getScmConnector(), scmGitFileTaskParams.getFoldersList(),
                scmGitFileTaskParams.getBranchName(), SCMGrpc.newBlockingStub(c)));
        return GitFileTaskResponseData.builder()
            .gitFileTaskType(scmGitFileTaskParams.getGitFileTaskType())
            .fileBatchContentResponse(fileBatchContentResponse)
            .build();
      case GET_FILE_CONTENT:
        final FileContent fileContent = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.getFileContent(scmGitFileTaskParams.getScmConnector(),
                scmGitFileTaskParams.getGitFilePathDetails(), SCMGrpc.newBlockingStub(c)));
        return GitFileTaskResponseData.builder()
            .gitFileTaskType(scmGitFileTaskParams.getGitFileTaskType())
            .fileContent(fileContent)
            .build();
      default:
        throw new UnknownEnumTypeException("GitFileTaskType", scmGitFileTaskParams.getGitFileTaskType().toString());
    }
  }
}
