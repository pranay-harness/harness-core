package software.wings.service.intfc;

import freemarker.template.TemplateException;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.waitnotify.NotifyResponseData;

import java.io.File;
import java.io.IOException;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public interface DelegateService {
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

  Delegate get(String accountId, String delegateId);

  Delegate update(Delegate delegate);

  Delegate checkForUpgrade(String accountId, String delegateId, String version, String managerHost)
      throws IOException, TemplateException;

  Delegate add(Delegate delegate);

  void delete(String accountId, String delegateId);

  Delegate register(Delegate delegate);

  void queueTask(DelegateTask task);

  <T extends NotifyResponseData> T executeTask(DelegateTask task) throws InterruptedException;

  PageResponse<DelegateTask> getDelegateTasks(String accountId, String delegateId);

  boolean acquireDelegateTask(String accountId, String delegateId, String taskId);

  void processDelegateResponse(DelegateTaskResponse response);

  File download(String managerHost, String accountId) throws IOException, TemplateException;

  boolean filter(String delegateId, DelegateTask task);
}
