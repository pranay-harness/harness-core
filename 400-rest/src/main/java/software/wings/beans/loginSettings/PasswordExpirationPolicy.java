/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.loginSettings;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class PasswordExpirationPolicy {
  private boolean enabled;
  private int daysBeforePasswordExpires;
  private int daysBeforeUserNotifiedOfPasswordExpiration;
}
