package software.wings.service.intfc;

import freemarker.template.TemplateException;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.waitnotify.NotifyResponseData;

import java.io.File;
import java.io.IOException;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public interface DelegateService {
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

  Delegate get(String accountId, String delegateId);

  Delegate update(@Valid Delegate delegate);

  Delegate checkForUpgrade(String accountId, String delegateId, String version, String managerHost)
      throws IOException, TemplateException;

  Delegate add(Delegate delegate);

  void delete(String accountId, String delegateId);

  Delegate register(@Valid Delegate delegate);

  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  <T extends NotifyResponseData> T executeTask(@Valid DelegateTask task) throws InterruptedException;

  PageResponse<DelegateTask> getDelegateTasks(String accountId, String delegateId);

  DelegateTask acquireDelegateTask(String accountId, String delegateId, String taskId);

  DelegateTask startDelegateTask(String accountId, String delegateId, String taskId);

  void processDelegateResponse(@Valid DelegateTaskResponse response);

  File download(String managerHost, String accountId) throws IOException, TemplateException;

  boolean filter(String delegateId, DelegateTask task);

  boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent);

  void abortTask(String accountId, String delegateTaskId);
}
