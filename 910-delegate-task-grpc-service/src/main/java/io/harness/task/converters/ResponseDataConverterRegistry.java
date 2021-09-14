/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
