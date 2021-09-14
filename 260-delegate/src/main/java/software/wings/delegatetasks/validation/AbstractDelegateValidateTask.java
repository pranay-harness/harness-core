/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.validation;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.network.Http.connectableHttpUrl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.TaskLogContext;

import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by brett on 11/1/17
 */
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public abstract class AbstractDelegateValidateTask implements DelegateValidateTask {
  protected String delegateTaskId;

  private String accountId;
  private String delegateId;
  private String taskType;
  private Consumer<List<DelegateConnectionResult>> consumer;
  private TaskData taskData;
  private List<ExecutionCapability> executionCapabilities;

  public AbstractDelegateValidateTask(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    this.accountId = delegateTaskPackage.getAccountId();
    this.delegateId = delegateId;
    this.delegateTaskId = delegateTaskPackage.getDelegateTaskId();
    this.taskType = delegateTaskPackage.getData().getTaskType();
    this.consumer = consumer;
    this.taskData = delegateTaskPackage.getData();
    this.executionCapabilities = delegateTaskPackage.getExecutionCapabilities();
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
        log.error("Unexpected error validating delegate task.", exception);
      } finally {
        if (consumer != null) {
          consumer.accept(results);
        }
      }
      return results;
    } catch (Exception e) {
      log.error("Unexpected error executing delegate task {}", delegateId, e);
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
