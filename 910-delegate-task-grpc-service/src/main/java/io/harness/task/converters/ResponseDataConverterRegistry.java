/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.task.converters;

import io.harness.exception.InvalidArgumentsException;
import io.harness.task.service.TaskType;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ResponseDataConverterRegistry {
  Map<TaskType, ResponseDataConverter<?>> registry = new ConcurrentHashMap<>();

  public <T> void register(TaskType taskType, ResponseDataConverter<T> responseDataConverter) {
    if (registry.containsKey(taskType)) {
      throw new InvalidArgumentsException("Duplicate registration for type: " + taskType);
    }
    registry.put(taskType, responseDataConverter);
  }

  public <T> ResponseDataConverter<T> obtain(TaskType taskType) {
    if (registry.containsKey(taskType)) {
      return (ResponseDataConverter<T>) registry.get(taskType);
    }
    throw new InvalidArgumentsException("No converter registered for type: " + taskType);
  }
}
