package io.harness.gitsync.gittoharness;

import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.ProcessingResponse;

import java.util.List;

/**
 * Implement this interface if you want a handle pre and post processing operation.
 */
public interface ChangeSetInterceptorService {
  void onChangeSetReceive(ChangeSets changeSets, String accountId);

  void postChangeSetProcessing(ProcessingResponse fileProcessingResponse, String accountId);

  void postChangeSetSort(List<ChangeSet> changeSetList, String accountId);
}
