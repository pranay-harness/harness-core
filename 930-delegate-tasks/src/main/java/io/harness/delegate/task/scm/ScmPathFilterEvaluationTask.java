package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.ngtriggers.conditionchecker.ConditionEvaluator;
import io.harness.product.ci.scm.proto.FileChange;
import io.harness.product.ci.scm.proto.FindFilesInCommitResponse;
import io.harness.product.ci.scm.proto.FindFilesInPRResponse;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.PRFile;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(HarnessTeam.PIPELINE)
public class ScmPathFilterEvaluationTask extends AbstractDelegateRunnableTask {
  private static final ScmPathFilterEvaluationTaskResponseData NOT_A_MATCH =
      ScmPathFilterEvaluationTaskResponseData.builder().matched(false).build();
  private static final ScmPathFilterEvaluationTaskResponseData MATCHED_ALL =
      ScmPathFilterEvaluationTaskResponseData.builder().matched(false).build();

  @Inject ScmServiceClient scmServiceClient;
  @Inject ScmDelegateClient scmDelegateClient;

  public ScmPathFilterEvaluationTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmPathFilterEvaluationTaskParams filterQueryParams = (ScmPathFilterEvaluationTaskParams) parameters;
    Set<String> changedFiles = getChangedFileset(filterQueryParams);

    for (String filepath : changedFiles) {
      if (ConditionEvaluator.evaluate(filepath, filterQueryParams.getOperator(), filterQueryParams.getStandard())) {
        return MATCHED_ALL;
      }
    }
    return NOT_A_MATCH;
  }

  private Set<String> getChangedFileset(ScmPathFilterEvaluationTaskParams params) {
    if (params.getPrNumber() != 0) {
      // PR case
      FindFilesInPRResponse findFilesResponse = scmDelegateClient.processScmRequest(c
          -> scmServiceClient.findFilesInPR(
              params.getScmConnector(), params.getPrNumber(), SCMGrpc.newBlockingStub(c)));
      Set<String> filepaths = new HashSet<>();
      for (PRFile prfile : findFilesResponse.getFilesList()) {
        filepaths.add(prfile.getPath());
      }
      return filepaths;
    } else {
      // push case
      ListCommitsResponse listCommitsResponse = scmDelegateClient.processScmRequest(
          c -> scmServiceClient.listCommits(params.getScmConnector(), params.getBranch(), SCMGrpc.newBlockingStub(c)));
      Set<String> filepaths = new HashSet<>();
      boolean inRange = false;
      for (String commitId : listCommitsResponse.getCommitIdsList()) {
        if (commitId.equals(params.getPreviousCommit())) {
          return filepaths;
        } else if (!inRange && commitId.equals(params.getPreviousCommit())) {
          inRange = true;
        }
        if (inRange) {
          FindFilesInCommitResponse findFilesInCommitResponse = scmDelegateClient.processScmRequest(
              c -> scmServiceClient.findFilesInCommit(params.getScmConnector(), commitId, SCMGrpc.newBlockingStub(c)));
          for (FileChange fileChange : findFilesInCommitResponse.getFileList()) {
            filepaths.add(fileChange.getPath());
          }
        }
        return filepaths;
      }
      return new HashSet<>();
    }
  }
}
