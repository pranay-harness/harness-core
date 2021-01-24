package software.wings.exception;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._870_YAML_BEANS)
public class IncompleteStateException extends RuntimeException {
  public IncompleteStateException(String message) {
    super(message);
  }
}
