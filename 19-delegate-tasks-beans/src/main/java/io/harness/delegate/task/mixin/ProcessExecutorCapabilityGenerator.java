package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProcessExecutorCapabilityGenerator {
  private static final Logger logger = LoggerFactory.getLogger(ProcessExecutorCapabilityGenerator.class);

  public static ProcessExecutorCapability buildProcessExecutorCapability(
      String category, List<String> processExecutorArguments) {
    return ProcessExecutorCapability.builder()
        .category(category)
        .processExecutorArguments(processExecutorArguments)
        .build();
  }
}
