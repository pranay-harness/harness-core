package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public enum SelectorType {
  PROFILE_NAME,
  DELEGATE_NAME,
  HOST_NAME,
  GROUP_NAME,
  PROFILE_SELECTORS
}
