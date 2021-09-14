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

@Data
@Builder
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class PasswordStrengthPolicy {
  private boolean enabled;
  private int minNumberOfCharacters;
  private int minNumberOfUppercaseCharacters;
  private int minNumberOfLowercaseCharacters;
  private int minNumberOfSpecialCharacters;
  private int minNumberOfDigits;
}
