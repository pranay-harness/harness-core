package software.wings.beans.loginSettings;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public enum PasswordSource {
  SIGN_UP_FLOW,
  PASSWORD_RESET_FLOW;
}
