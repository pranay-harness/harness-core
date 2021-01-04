package software.wings.delegatetasks.cv;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._930_DELEGATE_TASKS)
public class DataCollectionException extends RuntimeException {
  public DataCollectionException(Exception e) {
    super(e);
  }

  public DataCollectionException(String message) {
    super(message);
  }

  public DataCollectionException(String message, Exception e) {
    super(message, e);
  }
}
