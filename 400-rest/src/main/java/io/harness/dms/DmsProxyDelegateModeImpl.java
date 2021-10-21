package io.harness.dms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.selection.log.BatchDelegateSelectionLog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@OwnedBy(HarnessTeam.DEL)
public class DmsProxyDelegateModeImpl implements DmsProxy {
  @Override
  public boolean sampleDelegateExists(String accountId) {
    return false;
  }

  @Override
  public List<Delegate> getNonDeletedDelegatesForAccount(String accountId) {
    return null;
  }

  @Override
  public List<Integer> getCountOfDelegatesForAccounts(List<String> collect) {
    return null;
  }

  @Override
  public String queueTask(DelegateTask task) {
    return null;
  }

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return null;
  }

  @Override
  public void deleteByAccountId(String accountId) {}

  @Override
  public boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, String accountId, String appId,
      String envId, String infraMappingId, TaskGroup taskGroup, List<ExecutionCapability> executionCapabilities,
      Map<String, String> taskSetupAbstractions) {
    return false;
  }

  @Override
  public Optional<DelegateSelectionLogParams> fetchSelectedDelegateForTask(String accountId, String taskId) {
    return Optional.empty();
  }

  @Override
  public Delegate get(String accountId, String delegateId, boolean forceRefresh) {
    return null;
  }
}
