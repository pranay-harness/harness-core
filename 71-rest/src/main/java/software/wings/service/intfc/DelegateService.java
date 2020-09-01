package software.wings.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.ConnectionMode;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskAbortEvent;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.validation.Create;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.mongodb.morphia.query.Query;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Delegate;
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

public interface DelegateService extends OwnedByAccount {
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

  boolean checkDelegateConnected(String accountId, String delegateId);

  List<String> getKubernetesDelegateNames(String accountId);

  Set<String> getAllDelegateSelectors(String accountId);

  DelegateStatus getDelegateStatus(String accountId);

  DelegateStatus getDelegateStatusWithScalingGroups(String accountId);

  Set<String> retrieveDelegateSelectors(Delegate delegate);

  List<String> getAvailableVersions(String accountId);

  Delegate get(String accountId, String delegateId, boolean forceRefresh);

  Delegate update(@Valid Delegate delegate);

  Delegate updateTags(@Valid Delegate delegate);

  Delegate updateDescription(String accountId, String delegateId, String newDescription);

  Delegate updateApprovalStatus(String accountId, String delegateId, DelegateApproval action);

  Delegate updateScopes(@Valid Delegate delegate);

  DelegateScripts getDelegateScripts(String accountId, String version, String managerHost, String verificationHost)
      throws IOException;

  String getLatestDelegateVersion(String accountId);

  File downloadScripts(String managerHost, String verificationServiceUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException;

  File downloadDocker(String managerHost, String verificationServiceUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException;

  File downloadKubernetes(String managerHost, String verificationServiceUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException;

  File downloadCeKubernetesYaml(String managerHost, String verificationUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException;

  File downloadECSDelegate(String managerHost, String verificationUrl, String accountId, boolean awsVpcMode,
      String hostname, String delegateGroupName, String delegateProfile) throws IOException;
  Delegate add(Delegate delegate);

  void delete(String accountId, String delegateId);

  void retainOnlySelectedDelegatesAndDeleteRest(String accountId, List<String> delegatesToRetain);

  DelegateRegisterResponse register(@Valid Delegate delegate);

  DelegateRegisterResponse register(@Valid DelegateParams delegateParams);

  void registerHeartbeat(
      String accountId, String delegateId, DelegateConnectionHeartbeat heartbeat, ConnectionMode mode);

  DelegateProfileParams checkForProfile(String accountId, String delegateId, String profileId, long lastUpdatedAt);

  void saveProfileResult(String accountId, String delegateId, boolean error, FileBucket fileBucket,
      InputStream uploadedInputStream, FormDataContentDisposition fileDetail);

  String getProfileResult(String accountId, String delegateId);

  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  void scheduleSyncTask(DelegateTask task);

  <T extends ResponseData> T executeTask(DelegateTask task) throws InterruptedException;

  String obtainDelegateName(Delegate delegate);

  String obtainDelegateName(String accountId, String delegateId, boolean forceRefresh);

  void saveDelegateTask(DelegateTask task);

  DelegateTaskPackage acquireDelegateTask(String accountId, String delegateId, String taskId);

  DelegateTaskPackage reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results);

  void failIfAllDelegatesFailed(String accountId, String delegateId, String taskId);

  void clearCache(String accountId, String delegateId);

  void processDelegateResponse(
      String accountId, String delegateId, String taskId, @Valid DelegateTaskResponse response);

  boolean filter(String accountId, String delegateId);

  boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent);

  DelegateTask abortTask(String accountId, String delegateTaskId);

  String expireTask(String accountId, String delegateTaskId);

  List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly);

  Delegate updateHeartbeatForDelegateWithPollingEnabled(Delegate delegate);

  Delegate handleEcsDelegateRequest(Delegate delegate);

  File downloadDelegateValuesYamlFile(String managerHost, String verificationUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException;

  List<Integer> getCountOfDelegatesForAccounts(List<String> collect);

  Optional<DelegateTask> fetchDelegateTask(String accountId, String taskId);

  void handleResponse(DelegateTask delegateTask, Query<DelegateTask> taskQuery, DelegateTaskResponse response);

  boolean validateThatDelegateNameIsUnique(String accountId, String delegateName);

  void delegateDisconnected(String accountId, String delegateId, String delegateConnectionId);
}
