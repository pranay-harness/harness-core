package software.wings.delegatetasks.cv;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._930_DELEGATE_TASKS)
public class RateLimitExceededException extends DataCollectionException {
  public RateLimitExceededException(Exception e) {
    super(e);
  }

  public RateLimitExceededException(String message) {
    super(message);
  }
}
