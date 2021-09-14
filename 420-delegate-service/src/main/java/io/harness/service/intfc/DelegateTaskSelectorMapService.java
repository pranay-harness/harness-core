/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;

import java.util.List;

public interface DelegateTaskSelectorMapService {
  /**
   * List page response.
   *
   * @return the page response
   */
  List<TaskSelectorMap> list(String accountId);

  /**
   * Add task selector map.
   *
   * @param taskSelectorMap the task selector map
   * @return the delegate scope
   */
  TaskSelectorMap add(TaskSelectorMap taskSelectorMap);

  /**
   * Update task selector map.
   *
   * @param taskSelectorMap
   * @return the delegate scope
   */
  TaskSelectorMap update(TaskSelectorMap taskSelectorMap);

  /**
   * Add task selector for category.
   *
   * @param accountId account id
   * @param taskSelectorMapUuid uuid of task selector map
   * @param taskSelector task selector to add
   * @return updated task selector map
   */
  TaskSelectorMap addTaskSelector(String accountId, String taskSelectorMapUuid, String taskSelector);

  /**
   * Remove task selector for category. If no task selectors are left, this will delete the whole map and return null.
   *
   * @param accountId  account id
   * @param taskSelectorMapUuid uuid of task selector
   * @param taskSelector task selector to remove
   * @return updated task selector map, or null if map is deleted as result.
   */
  TaskSelectorMap removeTaskSelector(String accountId, String taskSelectorMapUuid, String taskSelector);

  /**
   * Get task selector map given task group.
   *
   * @param accountId
   * @param taskGroup
   * @return
   */
  TaskSelectorMap get(String accountId, TaskGroup taskGroup);
}
