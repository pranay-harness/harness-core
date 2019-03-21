package io.harness.delegate.task;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// TODO: This is temparary solution till all DelegateValidationTasks are moved to
// TODO: New Capability Framework. This should go away once that happens.
public class CapabilityUtil {
  private static Set<String> taskTypesMigratedToCapabilityFramework = new HashSet<>(Arrays.asList("HTTP", "JENKINS",
      "JENKINS_COLLECTION", "JENKINS_GET_BUILDS", "JENKINS_GET_JOBS", "JENKINS_GET_JOB", "JENKINS_GET_ARTIFACT_PATHS",
      "JENKINS_LAST_SUCCESSFUL_BUILD", "JENKINS_GET_PLANS", "JENKINS_VALIDATE_ARTIFACT_SERVER",

      "GCS_GET_ARTIFACT_PATHS", "GCS_GET_BUILDS", "GCS_GET_BUCKETS", "GCS_GET_PLANS",

      "BAMBOO", "BAMBOO_COLLECTION", "BAMBOO_GET_BUILDS", "BAMBOO_GET_JOBS", "BAMBOO_GET_ARTIFACT_PATHS",
      "BAMBOO_LAST_SUCCESSFUL_BUILD", "BAMBOO_GET_PLANS", "BAMBOO_VALIDATE_ARTIFACT_SERVER"));

  public static boolean isTaskTypeMigratedToCapabilityFramework(String taskType) {
    return taskTypesMigratedToCapabilityFramework.contains(taskType);
  }
}
