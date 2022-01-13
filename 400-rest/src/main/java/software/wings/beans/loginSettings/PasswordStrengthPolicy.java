/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.loginSettings;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
@OwnedBy(HarnessTeam.PL)
@Schema(description = "This has information about the password strength policy in Harness.")
public class PasswordStrengthPolicy {
  @Schema(description = "This value is true if the password strength policy is enabled. Otherwise, it is false.")
  private boolean enabled;
  @Schema(description = "Minimum number of characters required in a password.") private int minNumberOfCharacters;
  @Schema(description = "Minimum number of uppercase characters required in a password.")
  private int minNumberOfUppercaseCharacters;
  @Schema(description = "Minimum number of lower characters required in a password.")
  private int minNumberOfLowercaseCharacters;
  @Schema(description = "Minimum number of special characters required in a password.")
  private int minNumberOfSpecialCharacters;
  @Schema(description = "Minimum number of digits required in a password.") private int minNumberOfDigits;
}
