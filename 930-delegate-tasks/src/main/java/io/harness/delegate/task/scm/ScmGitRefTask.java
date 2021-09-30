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
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.product.ci.scm.proto.CompareCommitsResponse;
import io.harness.product.ci.scm.proto.FindPRResponse;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListCommitsInPRResponse;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(HarnessTeam.DX)
public class ScmGitRefTask extends AbstractDelegateRunnableTask {
  @Inject SecretDecryptionService secretDecryptionService;
  @Inject ScmDelegateClient scmDelegateClient;
  @Inject ScmServiceClient scmServiceClient;

  public ScmGitRefTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmGitRefTaskParams scmGitRefTaskParams = (ScmGitRefTaskParams) parameters;
    secretDecryptionService.decrypt(
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmGitRefTaskParams.getScmConnector()),
        scmGitRefTaskParams.getEncryptedDataDetails());
    switch (scmGitRefTaskParams.getGitRefType()) {
      case BRANCH: {
        ListBranchesResponse listBranchesResponse = scmDelegateClient.processScmRequest(
            c -> scmServiceClient.listBranches(scmGitRefTaskParams.getScmConnector(), SCMGrpc.newBlockingStub(c)));
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
            listBranchesResponse.getStatus(), listBranchesResponse.getError());
        return ScmGitRefTaskResponseData.builder()
            .gitRefType(scmGitRefTaskParams.getGitRefType())
            .listBranchesResponse(listBranchesResponse.toByteArray())
            .build();
      }
      case COMMIT: {
        ListCommitsResponse listCommitsResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.listCommits(
                scmGitRefTaskParams.getScmConnector(), scmGitRefTaskParams.getBranch(), SCMGrpc.newBlockingStub(c)));
        return ScmGitRefTaskResponseData.builder()
            .gitRefType(scmGitRefTaskParams.getGitRefType())
            .branch(scmGitRefTaskParams.getBranch())
            .listCommitsResponse(listCommitsResponse.toByteArray())
            .build();
      }
      case PULL_REQUEST_COMMITS: {
        ListCommitsInPRResponse listCommitsInPRResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.listCommitsInPR(
                scmGitRefTaskParams.getScmConnector(), scmGitRefTaskParams.getPrNumber(), SCMGrpc.newBlockingStub(c)));
        return ScmGitRefTaskResponseData.builder()
            .gitRefType(scmGitRefTaskParams.getGitRefType())
            .listCommitsInPRResponse(listCommitsInPRResponse.toByteArray())
            .build();
      }
      case PULL_REQUEST_WITH_COMMITS: {
        FindPRResponse findPRResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.findPR(
                scmGitRefTaskParams.getScmConnector(), scmGitRefTaskParams.getPrNumber(), SCMGrpc.newBlockingStub(c)));
        ListCommitsInPRResponse listCommitsInPRResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.listCommitsInPR(
                scmGitRefTaskParams.getScmConnector(), scmGitRefTaskParams.getPrNumber(), SCMGrpc.newBlockingStub(c)));
        return ScmGitRefTaskResponseData.builder()
            .gitRefType(scmGitRefTaskParams.getGitRefType())
            .repoUrl(scmGitRefTaskParams.getScmConnector().getUrl())
            .findPRResponse(findPRResponse.toByteArray())
            .listCommitsInPRResponse(listCommitsInPRResponse.toByteArray())
            .build();
      }
      case COMPARE_COMMITS: {
        CompareCommitsResponse compareCommitsResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.compareCommits(scmGitRefTaskParams.getScmConnector(),
                scmGitRefTaskParams.getInitialCommitId(), scmGitRefTaskParams.getFinalCommitId(),
                SCMGrpc.newBlockingStub(c)));
        return ScmGitRefTaskResponseData.builder()
            .gitRefType(scmGitRefTaskParams.getGitRefType())
            .compareCommitsResponse(compareCommitsResponse.toByteArray())
            .build();
      }
      case LATEST_COMMIT_ID: {
        final GetLatestCommitResponse latestCommitResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.getLatestCommit(scmGitRefTaskParams.getScmConnector(), scmGitRefTaskParams.getBranch(),
                scmGitRefTaskParams.getRef(), SCMGrpc.newBlockingStub(c)));
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
            latestCommitResponse.getStatus(), latestCommitResponse.getError());
        return ScmGitRefTaskResponseData.builder()
            .branch(scmGitRefTaskParams.getBranch())
            .repoUrl(scmGitRefTaskParams.getScmConnector().getUrl())
            .gitRefType(scmGitRefTaskParams.getGitRefType())
            .getLatestCommitResponse(latestCommitResponse.toByteArray())
            .build();
      }
      default:
        throw new UnknownEnumTypeException("GitRefType", scmGitRefTaskParams.getGitRefType().toString());
    }
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }
}
