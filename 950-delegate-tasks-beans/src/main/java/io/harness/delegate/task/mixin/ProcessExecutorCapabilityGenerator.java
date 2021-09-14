/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProcessExecutorCapabilityGenerator {
  public static ProcessExecutorCapability buildProcessExecutorCapability(
      String category, List<String> processExecutorArguments) {
    return ProcessExecutorCapability.builder()
        .category(category)
        .processExecutorArguments(processExecutorArguments)
        .build();
  }
}
