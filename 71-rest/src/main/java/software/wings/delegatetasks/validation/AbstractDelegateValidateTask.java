package software.wings.delegatetasks.validation;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.TaskLogContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/1/17
 */
@Slf4j
public abstract class AbstractDelegateValidateTask implements DelegateValidateTask {
  protected String delegateTaskId;

  private String accountId;
  private String delegateId;
  private String taskType;
  private Consumer<List<DelegateConnectionResult>> consumer;
  private TaskData taskData;
  private List<ExecutionCapability> executionCapabilities;

  public AbstractDelegateValidateTask(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    this.accountId = delegateTask.getAccountId();
    this.delegateId = delegateId;
    this.delegateTaskId = delegateTask.getUuid();
    this.taskType = delegateTask.getData().getTaskType();
    this.consumer = consumer;
    this.taskData = delegateTask.getData();
    this.executionCapabilities = delegateTask.getExecutionCapabilities();
  }

  @Override
  public List<DelegateConnectionResult> validationResults() {
    try (TaskLogContext ignore = new TaskLogContext(this.delegateTaskId, OVERRIDE_ERROR)) {
      List<DelegateConnectionResult> results = null;
      try {
        long startTime = System.currentTimeMillis();
        results = validate();
        long duration = System.currentTimeMillis() - startTime;
        for (DelegateConnectionResult result : results) {
          result.setAccountId(accountId);
          result.setDelegateId(delegateId);
          if (result.getDuration() == 0) {
            result.setDuration(duration);
          }
        }
      } catch (Exception exception) {
        logger.error("Unexpected error validating delegate task.", exception);
      } finally {
        if (consumer != null) {
          consumer.accept(results);
        }
      }
      return results;
    } catch (Exception e) {
      logger.error("Unexpected error executing delegate task {}", delegateId, e);
    }
    return emptyList();
  }

  public List<DelegateConnectionResult> validate() {
    try {
      String criteria = getCriteria().get(0);
      return singletonList(
          DelegateConnectionResult.builder().criteria(criteria).validated(connectableHttpUrl(criteria)).build());
    } catch (Exception e) {
      return emptyList();
    }
  }

  public Object[] getParameters() {
    return getTaskData().getParameters();
  }

  protected String getTaskType() {
    return taskType;
  }

  protected TaskData getTaskData() {
    return taskData;
  }

  protected List<ExecutionCapability> getExecutionCapabilities() {
    return executionCapabilities;
  }
}
