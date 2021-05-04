package io.harness.delegate.task.scm;

import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.service.ScmServiceClient;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

public class ScmOtherOpsTask extends AbstractDelegateRunnableTask {
  private final SecretDecryptionService secretDecryptionService;
  private final ScmServiceClient scmServiceClient;
  private final ScmDelegateClient scmDelegateClient;

  public ScmOtherOpsTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute,
      SecretDecryptionService secretDecryptionService, ScmServiceClient scmServiceClient,
      ScmDelegateClient scmDelegateClient) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
    this.secretDecryptionService = secretDecryptionService;
    this.scmServiceClient = scmServiceClient;
    this.scmDelegateClient = scmDelegateClient;
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmOtherOpsTaskParams scmOtherOpsTaskParams = (ScmOtherOpsTaskParams) parameters;
    secretDecryptionService.decrypt(
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmOtherOpsTaskParams.getScmConnector()),
        scmOtherOpsTaskParams.getEncryptedDataDetails());
    switch (scmOtherOpsTaskParams.getScmOtherOpsType()) {
      case GET_FILE_CONTENT:
        FileContent fileContent = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.getFileContent(scmOtherOpsTaskParams.getScmConnector(),
                scmOtherOpsTaskParams.getGitFilePathDetails(), SCMGrpc.newBlockingStub(c)));
        return ScmOtherOpsTaskResponseData.builder()
            .scmOtherOpsType(ScmOtherOpsType.GET_FILE_CONTENT)
            .fileContent(fileContent)
            .build();
      default:
        throw new UnknownEnumTypeException("ScmOtherOpsType", scmOtherOpsTaskParams.getScmOtherOpsType().toString());
    }
  }
}
