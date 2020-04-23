package software.wings.delegatetasks.citasks.cik8handler;

import io.harness.logging.AutoLogContext;

public class CIK8LogContext extends AutoLogContext {
  public static final String ID = "PodName";

  public CIK8LogContext(String podName, OverrideBehavior behavior) {
    super(ID, podName, behavior);
  }
}
