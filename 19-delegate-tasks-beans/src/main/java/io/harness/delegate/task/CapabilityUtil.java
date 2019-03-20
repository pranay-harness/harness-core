package io.harness.delegate.task;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// TODO: This is temparary solution till all DelegateValidationTasks are moved to
// TODO: New Capability Framework. This should go away once that happens.
public class CapabilityUtil {
  private static Set<String> taskTypesMigratedToCapabilityFramework = new HashSet<>(Arrays.asList("HTTP"));

  public static boolean isTaskTypeMigratedToCapabilityFramework(String taskType) {
    return taskTypesMigratedToCapabilityFramework.contains(taskType);
  }
}
