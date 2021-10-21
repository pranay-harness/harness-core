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
import io.harness.service.intfc.DelegateCache;

import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.DEL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Singleton
public class DmsProxyManagerModeImpl implements DmsProxy {
  DelegateService delegateService;
  AssignDelegateService assignDelegateService;
  DelegateSelectionLogsService delegateSelectionLogsService;
  DelegateCache delegateCache;

  @Override
  public boolean sampleDelegateExists(String accountId) {
    return delegateService.sampleDelegateExists(accountId);
  }

  @Override
  public List<Delegate> getNonDeletedDelegatesForAccount(String accountId) {
    return delegateService.getNonDeletedDelegatesForAccount(accountId);
  }

  @Override
  public List<Integer> getCountOfDelegatesForAccounts(List<String> collect) {
    return delegateService.getCountOfDelegatesForAccounts(collect);
  }

  @Override
  public String queueTask(DelegateTask task) {
    return delegateService.queueTask(task);
  }

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return delegateService.list(pageRequest);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    delegateService.deleteByAccountId(accountId);
  }

  @Override
  public boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, String accountId, String appId,
      String envId, String infraMappingId, TaskGroup taskGroup, List<ExecutionCapability> executionCapabilities,
      Map<String, String> taskSetupAbstractions) {
    return assignDelegateService.canAssign(batch, delegateId, accountId, appId, envId, infraMappingId, taskGroup,
        executionCapabilities, taskSetupAbstractions);
  }

  @Override
  public Optional<DelegateSelectionLogParams> fetchSelectedDelegateForTask(String accountId, String taskId) {
    return delegateSelectionLogsService.fetchSelectedDelegateForTask(accountId, taskId);
  }

  @Override
  public Delegate get(String accountId, String delegateId, boolean forceRefresh) {
    return delegateCache.get(accountId, delegateId, forceRefresh);
  }
}
