/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.loginSettings;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public enum PasswordSource {
  SIGN_UP_FLOW,
  PASSWORD_RESET_FLOW;
}
