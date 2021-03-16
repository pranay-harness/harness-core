package io.harness.gitsync.gittoharness;

import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.ProcessingResponse;

import java.util.List;

public class NoOpChangeSetInterceptorServiceImpl implements ChangeSetInterceptorService {
  @Override
  public void onChangeSetReceive(ChangeSets changeSets, String accountId) {}

  @Override
  public void postChangeSetProcessing(ProcessingResponse fileProcessingResponse, String accountId) {}

  @Override
  public void postChangeSetSort(List<ChangeSet> changeSetList, String accountId) {}
}
