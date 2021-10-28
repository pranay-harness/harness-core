package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskSelectorMap.TaskSelectorMapKeys;
import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.persistence.DMSConstants.DMS;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NoResultFoundException;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTaskSelectorMapService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.DuplicateKeyException;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateTaskSelectorMapServiceImpl implements DelegateTaskSelectorMapService {
  @Inject @Named(DMS) private HPersistence persistence;

  @Override
  public List<TaskSelectorMap> list(String accountId) {
    return persistence.createQuery(TaskSelectorMap.class).filter(TaskSelectorMapKeys.accountId, accountId).asList();
  }

  @Override
  public TaskSelectorMap add(TaskSelectorMap taskSelectorMap) {
    log.info("Adding task selector map:" + taskSelectorMap);
    if (isEmpty(taskSelectorMap.getSelectors())) {
      log.warn("Task selector list cannot be empty.");
      throw new IllegalArgumentException("Task selector list cannot be empty.");
    }

    try {
      persistence.save(taskSelectorMap);
    } catch (DuplicateKeyException e) {
      ignoredOnPurpose(e);
      throw new InvalidRequestException("Task selector map with given task group already exists for this account");
    }

    log.info("Added task selector map: {}", taskSelectorMap.getUuid());
    return taskSelectorMap;
  }

  @Override
  public TaskSelectorMap update(TaskSelectorMap taskSelectorMap) {
    log.info("Updating task selector map:" + taskSelectorMap);
    if (isEmpty(taskSelectorMap.getSelectors())) {
      persistence.delete(TaskSelectorMap.class, taskSelectorMap.getUuid());
      return null;
    } else {
      persistence.update(taskSelectorMap,
          persistence.createUpdateOperations(TaskSelectorMap.class)
              .set(TaskSelectorMapKeys.selectors, taskSelectorMap.getSelectors()));
      return taskSelectorMap;
    }
  }

  @Override
  public TaskSelectorMap addTaskSelector(String accountId, String taskSelectorMapUuid, String taskSelector) {
    TaskSelectorMap existingMap = persistence.createQuery(TaskSelectorMap.class)
                                      .filter(TaskSelectorMapKeys.accountId, accountId)
                                      .filter(ID_KEY, taskSelectorMapUuid)
                                      .get();
    if (existingMap == null) {
      String errorMessage = String.format("Task selector map with id: %s not found", taskSelectorMapUuid);
      log.warn(errorMessage);
      throw NoResultFoundException.newBuilder().code(ErrorCode.RESOURCE_NOT_FOUND).message(errorMessage).build();
    }
    if (!existingMap.getSelectors().contains(taskSelector)) {
      existingMap.getSelectors().add(taskSelector);
      persistence.save(existingMap);
      log.info("Updated task selector map {} for task category {} with new task selector", taskSelectorMapUuid,
          existingMap.getTaskGroup(), taskSelector);
    }
    return existingMap;
  }

  @Override
  public TaskSelectorMap removeTaskSelector(String accountId, String taskSelectorMapUuid, String taskSelector) {
    TaskSelectorMap existingMap = persistence.createQuery(TaskSelectorMap.class)
                                      .filter(TaskSelectorMapKeys.accountId, accountId)
                                      .filter(ID_KEY, taskSelectorMapUuid)
                                      .get();
    if (existingMap == null) {
      String errorMessage = String.format("Task selector map with id: %s not found", taskSelectorMapUuid);
      log.warn(errorMessage);
      throw NoResultFoundException.newBuilder().code(ErrorCode.RESOURCE_NOT_FOUND).message(errorMessage).build();
    }
    if (isNotEmpty(existingMap.getSelectors()) && existingMap.getSelectors().contains(taskSelector)) {
      existingMap.getSelectors().remove(taskSelector);
      if (existingMap.getSelectors().isEmpty()) {
        persistence.delete(TaskSelectorMap.class, taskSelectorMapUuid);
        existingMap = null;
        log.info("Delegate task selector map {} deleted", taskSelectorMapUuid);
      } else {
        persistence.save(existingMap);
        log.info("Delegate task selector map {} updated", taskSelectorMapUuid);
      }
    }
    return existingMap;
  }

  @Override
  public TaskSelectorMap get(String accountId, TaskGroup taskGroup) {
    return persistence.createQuery(TaskSelectorMap.class)
        .filter(TaskSelectorMapKeys.accountId, accountId)
        .filter(TaskSelectorMapKeys.taskGroup, taskGroup)
        .get();
  }
}
