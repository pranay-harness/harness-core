package software.wings.expression;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._940_SECRET_MANAGER_CLIENT)
public enum SecretManagerMode {
  APPLY,
  DRY_RUN,
  CHECK_FOR_SECRETS
}
