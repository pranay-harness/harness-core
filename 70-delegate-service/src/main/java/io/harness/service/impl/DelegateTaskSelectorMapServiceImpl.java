package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskSelectorMap.TaskSelectorMapKeys;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.eraro.ErrorCode;
import io.harness.exception.NoResultFoundException;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTaskSelectorMapService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateTaskSelectorMapServiceImpl implements DelegateTaskSelectorMapService {
  @Inject private HPersistence hPersistence;

  @Override
  public List<TaskSelectorMap> list() {
    return hPersistence.createQuery(TaskSelectorMap.class).asList();
  }

  @Override
  public TaskSelectorMap add(TaskSelectorMap taskSelectorMap) {
    logger.info("Adding task selector map:" + taskSelectorMap);
    if (isEmpty(taskSelectorMap.getSelectors())) {
      logger.warn("Task selector list cannot be empty.");
      throw new IllegalArgumentException("Task selector list cannot be empty.");
    }
    TaskSelectorMap existing = hPersistence.createQuery(TaskSelectorMap.class)
                                   .filter(TaskSelectorMapKeys.accountId, taskSelectorMap.getAccountId())
                                   .filter(TaskSelectorMapKeys.taskGroup, taskSelectorMap.getTaskGroup())
                                   .get();
    if (existing == null) {
      hPersistence.save(taskSelectorMap);
      existing = taskSelectorMap;
    } else {
      existing.setSelectors(taskSelectorMap.getSelectors());
      hPersistence.update(existing,
          hPersistence.createUpdateOperations(TaskSelectorMap.class)
              .set(TaskSelectorMapKeys.selectors, taskSelectorMap.getSelectors()));
    }

    logger.info("Added task selector map: {}", taskSelectorMap.getUuid());
    return existing;
  }

  @Override
  public TaskSelectorMap update(TaskSelectorMap taskSelectorMap) {
    logger.info("Updating task selector map:" + taskSelectorMap);
    if (isEmpty(taskSelectorMap.getSelectors())) {
      hPersistence.delete(TaskSelectorMap.class, taskSelectorMap.getUuid());
      return null;
    } else {
      hPersistence.update(taskSelectorMap,
          hPersistence.createUpdateOperations(TaskSelectorMap.class)
              .set(TaskSelectorMapKeys.selectors, taskSelectorMap.getSelectors()));
      return taskSelectorMap;
    }
  }

  @Override
  public TaskSelectorMap addTaskSelector(String accountId, String taskSelectorMapUuid, String taskSelector) {
    TaskSelectorMap existingMap = hPersistence.createQuery(TaskSelectorMap.class)
                                      .filter(TaskSelectorMapKeys.accountId, accountId)
                                      .filter(ID_KEY, taskSelectorMapUuid)
                                      .get();
    if (existingMap == null) {
      String errorMessage = String.format("Task selector map with id: %s not found", taskSelectorMapUuid);
      logger.warn(errorMessage);
      throw NoResultFoundException.newBuilder().code(ErrorCode.RESOURCE_NOT_FOUND).message(errorMessage).build();
    }
    if (!existingMap.getSelectors().contains(taskSelector)) {
      existingMap.getSelectors().add(taskSelector);
      hPersistence.save(existingMap);
      logger.info("Updated task selector map {} for task category {} with new task selector", taskSelectorMapUuid,
          existingMap.getTaskGroup(), taskSelector);
    }
    return existingMap;
  }

  @Override
  public TaskSelectorMap removeTaskSelector(String accountId, String taskSelectorMapUuid, String taskSelector) {
    TaskSelectorMap existingMap = hPersistence.createQuery(TaskSelectorMap.class)
                                      .filter(TaskSelectorMapKeys.accountId, accountId)
                                      .filter(ID_KEY, taskSelectorMapUuid)
                                      .get();
    if (existingMap == null) {
      String errorMessage = String.format("Task selector map with id: %s not found", taskSelectorMapUuid);
      logger.warn(errorMessage);
      throw NoResultFoundException.newBuilder().code(ErrorCode.RESOURCE_NOT_FOUND).message(errorMessage).build();
    }
    if (isNotEmpty(existingMap.getSelectors()) && existingMap.getSelectors().contains(taskSelector)) {
      existingMap.getSelectors().remove(taskSelector);
      if (existingMap.getSelectors().isEmpty()) {
        hPersistence.delete(TaskSelectorMap.class, taskSelectorMapUuid);
        existingMap = null;
        logger.info("Delegate task selector map {} deleted", taskSelectorMapUuid);
      } else {
        hPersistence.save(existingMap);
        logger.info("Delegate task selector map {} updated", taskSelectorMapUuid);
      }
    }
    return existingMap;
  }
}
