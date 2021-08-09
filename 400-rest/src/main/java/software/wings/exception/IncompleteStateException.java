package software.wings.exception;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._870_YAML_BEANS)
public class IncompleteStateException extends RuntimeException {
  public IncompleteStateException(String message) {
    super(message);
  }
}
