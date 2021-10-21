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
import io.harness.validation.Create;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

@OwnedBy(HarnessTeam.DEL)
public interface DmsProxy {
  // Delegate svc
  boolean sampleDelegateExists(String accountId);

  List<Delegate> getNonDeletedDelegatesForAccount(String accountId);

  List<Integer> getCountOfDelegatesForAccounts(List<String> collect);

  // delegate service already takes care of this method to route to correct svc. That logic has to be encapsulated
  // inside this method eventually.
  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

  void deleteByAccountId(String accountId);

  // AssignDelegateService
  boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, String accountId, String appId, String envId,
      String infraMappingId, TaskGroup taskGroup, List<ExecutionCapability> executionCapabilities,
      Map<String, String> taskSetupAbstractions);

  // delegateSelectionLogsService
  Optional<DelegateSelectionLogParams> fetchSelectedDelegateForTask(String accountId, String taskId);

  // delegateCache
  Delegate get(String accountId, String delegateId, boolean forceRefresh);
}
