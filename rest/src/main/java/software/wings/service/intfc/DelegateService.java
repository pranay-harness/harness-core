package software.wings.service.intfc;

import freemarker.template.TemplateException;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.io.File;
import java.io.IOException;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public interface DelegateService {
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

  Delegate get(String accountId, String delegateId);

  Delegate update(Delegate delegate);

  Delegate checkForUpgrade(String accountId, String delegateId);

  Delegate add(Delegate delegate);

  void delete(String accountId, String delegateId);

  Delegate register(Delegate delegate);

  void sendTaskWaitNotify(DelegateTask task);

  PageResponse<DelegateTask> getDelegateTasks(String accountId, String delegateId);

  void processDelegateResponse(DelegateTaskResponse response);

  File download(String managerHost, String accountId) throws IOException, TemplateException;
}
