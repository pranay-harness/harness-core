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
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.PIPELINE)
public class ScmFilterQueryTask extends AbstractDelegateRunnableTask {
  private static final ScmFilterQueryTaskResponseData NOT_A_MATCH =
      ScmFilterQueryTaskResponseData.builder().matched(false).build();
  private static final ScmFilterQueryTaskResponseData MATCHED_ALL =
      ScmFilterQueryTaskResponseData.builder().matched(false).build();

  @Inject ScmServiceClient scmServiceClient;
  @Inject ScmDelegateClient scmDelegateClient;

  public ScmFilterQueryTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmFilterQueryTaskParams filterQueryParams = (ScmFilterQueryTaskParams) parameters;
    Set<String> changedFiles = getChangedFileset(filterQueryParams);
    Set<Pair<String, String>> unmatchedConditions = new HashSet<>(filterQueryParams.getConditions());

    for (String filepath : changedFiles) {
      for (Pair<String, String> condition : unmatchedConditions) {
        if (ConditionEvaluator.evaluate(filepath, condition.getLeft(), condition.getRight())) {
          unmatchedConditions.remove(condition);
        }
      }
      if (unmatchedConditions.isEmpty()) {
        return MATCHED_ALL;
      }
    }
    return NOT_A_MATCH;
  }

  private Set<String> getChangedFileset(ScmFilterQueryTaskParams params) {
    if (params.getPrNumber() != 0) {
      // PR case
      FindFilesInPRResponse findFilesResponse = scmDelegateClient.processScmRequest(c
          -> scmServiceClient.findFilesInPR(
              params.getScmConnector(), params.getPrNumber(), SCMGrpc.newBlockingStub(c)));
      Set<String> filepaths = new HashSet<>();
      for (PRFile prfile : findFilesResponse.getFilesList()) {
        filepaths.add(prfile.getPath());
      }
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
          throw new NotImplementedException("scm does not support");
          // FindFilesInCommitResponse findFilesInCommitResponse = scmDelegateClient.processScmRequest(
          //     c -> scmServiceClient.findFilesInCommit(
          //         params.getScmConnector(), (), SCMGrpc.newBlockingStub(c)));
        }
      }
      return filepaths;
    }
    return new HashSet<>();
  }
}
