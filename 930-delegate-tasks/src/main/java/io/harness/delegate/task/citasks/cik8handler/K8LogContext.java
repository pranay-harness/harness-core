/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.citasks.cik8handler;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AutoLogContext;

public class K8LogContext extends AutoLogContext {
  public static final String podID = "PodName";
  public static final String containerID = "ContainerName";

  public K8LogContext(String podName, String containerName, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .put(podID, podName)
              .putIfNotNull(containerID, containerName)
              .build(),
        behavior);
  }
}
