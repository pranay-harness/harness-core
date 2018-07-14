package software.wings.service.intfc;

import freemarker.template.TemplateException;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.waitnotify.NotifyResponseData;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public interface DelegateService {
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

  List<String> getKubernetesDelegateNames(String accountId);

  DelegateStatus getDelegateStatus(String accountId);

  Delegate get(String accountId, String delegateId);

  Delegate update(@Valid Delegate delegate);

  Delegate updateDescription(String accountId, String delegateId, String newDescription);

  Delegate updateScopes(@Valid Delegate delegate);

  DelegateScripts checkForUpgrade(String accountId, String delegateId, String version, String managerHost)
      throws IOException, TemplateException;

  String getLatestDelegateVersion(String accountId);

  File downloadScripts(String managerHost, String accountId) throws IOException, TemplateException;
  File downloadDocker(String managerHost, String accountId) throws IOException, TemplateException;
  File downloadKubernetes(String managerHost, String accountId, String delegateName)
      throws IOException, TemplateException;

  Delegate add(Delegate delegate);

  void delete(String accountId, String delegateId);

  Delegate register(@Valid Delegate delegate);

  void removeDelegateConnection(String accountId, String delegateConnectionId);

  void doConnectionHeartbeat(String accountId, String delegateId, DelegateConnectionHeartbeat heartbeat);

  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  <T extends NotifyResponseData> T executeTask(DelegateTask task) throws InterruptedException;

  DelegateTask acquireDelegateTask(String accountId, String delegateId, String taskId);

  DelegateTask reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results);

  void clearCache(String delegateId);

  void processDelegateResponse(
      String accountId, String delegateId, String taskId, @Valid DelegateTaskResponse response);

  boolean filter(String delegateId, DelegateTask task);

  boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent);

  void abortTask(String accountId, String delegateTaskId);

  List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly);

  Delegate updateHeartbeat(String accountId, String delegateId);

  void sendAlertNotificationsForDownDelegates(String accountId, List<Delegate> delegates);

  void sendAlertNotificationsForNoActiveDelegates(String accountId);
}
