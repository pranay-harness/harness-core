package io.harness.delegate.service;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.delegate.beans.ConnectionMode;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateInitializationDetails;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateProgressData;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.delegate.beans.DelegateSize;
import io.harness.delegate.beans.DelegateSizeDetails;
import io.harness.delegate.beans.DelegateTaskAbortEvent;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.K8sConfigDetails;
import io.harness.validation.Create;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.CEDelegateStatus;
import software.wings.beans.DelegateStatus;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.ws.rs.core.MediaType;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@OwnedBy(DEL)
public interface DelegateService extends OwnedByAccount {
  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  void scheduleSyncTask(DelegateTask task);

  <T extends DelegateResponseData> T executeTask(DelegateTask task) throws InterruptedException;

  void saveDelegateTask(DelegateTask task, DelegateTask.Status status);

  String queueParkedTask(String accountId, String taskId);

  byte[] getParkedTaskResults(String accountId, String taskId, String driverId);

  DelegateTaskPackage acquireDelegateTask(String accountId, String delegateId, String taskId);

  DelegateTaskPackage reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results);

  void failIfAllDelegatesFailed(String accountId, String delegateId, String taskId);

  void publishTaskProgressResponse(
      String accountId, String driverId, String delegateTaskId, DelegateProgressData responseData);

  DelegateTask abortTask(String accountId, String delegateTaskId);

  String expireTask(String accountId, String delegateTaskId);

  List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly);

  Optional<DelegateTask> fetchDelegateTask(String accountId, String taskId);

  void convertToExecutionCapability(DelegateTask task);
}
