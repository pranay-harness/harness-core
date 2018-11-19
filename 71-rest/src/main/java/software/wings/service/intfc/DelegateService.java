package software.wings.service.intfc;

import freemarker.template.TemplateException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.validation.Create;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public interface DelegateService {
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

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
      throws IOException, TemplateException;

  String getLatestDelegateVersion(String accountId);

  File downloadScripts(String managerHost, String verificationServiceUrl, String accountId)
      throws IOException, TemplateException;
  File downloadDocker(String managerHost, String verificationServiceUrl, String accountId)
      throws IOException, TemplateException;
  File downloadKubernetes(String managerHost, String verificationServiceUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException, TemplateException;

  Delegate add(Delegate delegate);

  void delete(String accountId, String delegateId);

  Delegate register(@Valid Delegate delegate);

  DelegateProfileParams checkForProfile(String accountId, String delegateId, String profileId, long lastUpdatedAt);

  void saveProfileResult(String accountId, String delegateId, boolean error, FileBucket fileBucket,
      InputStream uploadedInputStream, FormDataContentDisposition fileDetail);

  String getProfileResult(String accountId, String delegateId);

  void removeDelegateConnection(String accountId, String delegateConnectionId);

  void doConnectionHeartbeat(String accountId, String delegateId, DelegateConnectionHeartbeat heartbeat);

  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  <T extends ResponseData> T executeTask(DelegateTask task) throws InterruptedException;

  DelegateTask acquireDelegateTask(String accountId, String delegateId, String taskId);

  DelegateTask reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results);

  DelegateTask failIfAllDelegatesFailed(String accountId, String delegateId, String taskId);

  void clearCache(String accountId, String delegateId);

  void processDelegateResponse(
      String accountId, String delegateId, String taskId, @Valid DelegateTaskResponse response);

  boolean filter(String delegateId, DelegateTask task);

  boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent);

  void abortTask(String accountId, String delegateTaskId);

  void expireTask(String accountId, String delegateTaskId);

  List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly);

  Delegate updateHeartbeat(String accountId, String delegateId);

  void sendAlertNotificationsForDownDelegates(String accountId, List<Delegate> delegates);

  void sendAlertNotificationsForNoActiveDelegates(String accountId);
}
