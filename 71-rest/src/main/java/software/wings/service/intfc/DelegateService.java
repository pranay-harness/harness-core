package software.wings.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.validation.Create;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;

public interface DelegateService extends OwnedByAccount {
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

  boolean isDelegateConnected(String delegateId);

  List<String> getKubernetesDelegateNames(String accountId);

  Set<String> getAllDelegateTags(String accountId);

  DelegateStatus getDelegateStatus(String accountId);

  List<String> getAvailableVersions(String accountId);

  Delegate get(String accountId, String delegateId, boolean forceRefresh);

  Delegate update(@Valid Delegate delegate);

  Delegate updateTags(@Valid Delegate delegate);

  Delegate updateDescription(String accountId, String delegateId, String newDescription);

  Delegate updateScopes(@Valid Delegate delegate);

  DelegateScripts getDelegateScripts(String accountId, String version, String managerHost, String verificationHost)
      throws IOException;

  String getLatestDelegateVersion(String accountId);

  File downloadScripts(String managerHost, String verificationServiceUrl, String accountId) throws IOException;
  File downloadDocker(String managerHost, String verificationServiceUrl, String accountId) throws IOException;
  File downloadKubernetes(String managerHost, String verificationServiceUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException;
  File downloadECSDelegate(String managerHost, String verificationUrl, String accountId, boolean awsVpcMode,
      String hostname, String delegateGroupName, String delegateProfile) throws IOException;
  Delegate add(Delegate delegate);

  void delete(String accountId, String delegateId);

  void retainOnlySelectedDelegatesAndDeleteRest(String accountId, List<String> delegatesToRetain);

  Delegate register(@Valid Delegate delegate);

  DelegateProfileParams checkForProfile(String accountId, String delegateId, String profileId, long lastUpdatedAt);

  void saveProfileResult(String accountId, String delegateId, boolean error, FileBucket fileBucket,
      InputStream uploadedInputStream, FormDataContentDisposition fileDetail);

  String getProfileResult(String accountId, String delegateId);

  void removeDelegateConnection(String accountId, String delegateConnectionId);

  void doConnectionHeartbeat(String accountId, String delegateId, DelegateConnectionHeartbeat heartbeat);

  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  <T extends ResponseData> T executeTask(DelegateTask task) throws InterruptedException;

  DelegateTaskPackage acquireDelegateTask(String accountId, String delegateId, String taskId);

  DelegateTaskPackage reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results);

  void failIfAllDelegatesFailed(String accountId, String delegateId, String taskId);

  void clearCache(String accountId, String delegateId);

  void processDelegateResponse(
      String accountId, String delegateId, String taskId, @Valid DelegateTaskResponse response);

  boolean filter(String accountId, String delegateId);

  boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent);

  void abortTask(String accountId, String delegateTaskId);

  void expireTask(String accountId, String delegateTaskId);

  List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly);

  Delegate updateHeartbeatForDelegateWithPollingEnabled(Delegate delegate);

  Delegate handleEcsDelegateRequest(Delegate delegate);

  File downloadDelegateValuesYamlFile(String managerHost, String verificationUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException;
}
