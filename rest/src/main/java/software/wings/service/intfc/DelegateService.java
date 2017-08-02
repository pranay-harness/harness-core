package software.wings.service.intfc;

import freemarker.template.TemplateException;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScripts;
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
  /**
   * List page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Delegate> list(PageRequest<Delegate> pageRequest);

  /**
   * Get delegate.
   *
   * @param accountId  the account id
   * @param delegateId the delegate id
   * @return the delegate
   */
  Delegate get(String accountId, String delegateId);

  /**
   * Update delegate.
   *
   * @param delegate the delegate
   * @return the delegate
   */
  Delegate update(@Valid Delegate delegate);

  /**
   * Check for upgrade delegate.
   *
   * @param accountId   the account id
   * @param delegateId  the delegate id
   * @param version     the version
   * @param managerHost the manager host
   * @return the delegate scripts
   * @throws IOException       the io exception
   * @throws TemplateException the template exception
   */
  DelegateScripts checkForUpgrade(String accountId, String delegateId, String version, String managerHost)
      throws IOException, TemplateException;

  /**
   * Download file.
   *
   * @param managerHost the manager host
   * @param accountId   the account id
   * @return the file
   * @throws IOException       the io exception
   * @throws TemplateException the template exception
   */
  File download(String managerHost, String accountId) throws IOException, TemplateException;

  /**
   * Add delegate.
   *
   * @param delegate the delegate
   * @return the delegate
   */
  Delegate add(Delegate delegate);

  /**
   * Delete.
   *
   * @param accountId  the account id
   * @param delegateId the delegate id
   */
  void delete(String accountId, String delegateId);

  /**
   * Register delegate.
   *
   * @param delegate the delegate
   * @return the delegate
   */
  Delegate register(@Valid Delegate delegate);

  /**
   * Queue task string.
   *
   * @param task the task
   * @return the string
   */
  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  /**
   * Execute task t.
   *
   * @param <T>  the type parameter
   * @param task the task
   * @return the t
   * @throws InterruptedException the interrupted exception
   */
  <T extends NotifyResponseData> T executeTask(DelegateTask task) throws InterruptedException;

  /**
   * Gets delegate tasks.
   *
   * @param accountId  the account id
   * @param delegateId the delegate id
   * @return the delegate tasks
   */
  PageResponse<DelegateTask> getDelegateTasks(String accountId, String delegateId);

  /**
   * Acquire delegate task delegate task.
   *
   * @param accountId  the account id
   * @param delegateId the delegate id
   * @param taskId     the task id
   * @return the delegate task
   */
  DelegateTask acquireDelegateTask(String accountId, String delegateId, String taskId);

  /**
   * Start delegate task delegate task.
   *
   * @param accountId  the account id
   * @param delegateId the delegate id
   * @param taskId     the task id
   * @return the delegate task
   */
  DelegateTask startDelegateTask(String accountId, String delegateId, String taskId);

  /**
   * Process delegate response.
   *
   * @param response the response
   */
  void processDelegateResponse(@Valid DelegateTaskResponse response);

  /**
   * Filter boolean.
   *
   * @param delegateId the delegate id
   * @param task       the task
   * @return the boolean
   */
  boolean filter(String delegateId, DelegateTask task);

  /**
   * Filter boolean.
   *
   * @param delegateId     the delegate id
   * @param taskAbortEvent the task abort event
   * @return the boolean
   */
  boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent);

  /**
   * Abort task.
   *
   * @param accountId      the account id
   * @param delegateTaskId the delegate task id
   */
  void abortTask(String accountId, String delegateTaskId);
}
