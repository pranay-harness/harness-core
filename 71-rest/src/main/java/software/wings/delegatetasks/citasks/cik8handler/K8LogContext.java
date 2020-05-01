package software.wings.delegatetasks.citasks.cik8handler;

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
