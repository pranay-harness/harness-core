package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;

import java.util.List;

public class ProcessExecutorCapabilityGenerator {
  public static ProcessExecutorCapability buildProcessExecutorCapability(
      String category, List<String> processExecutorArguments) {
    return ProcessExecutorCapability.builder()
        .category(category)
        .processExecutorArguments(processExecutorArguments)
        .build();
  }
}
