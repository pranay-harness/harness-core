package software.wings.beans.loginSettings;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
@OwnedBy(HarnessTeam.PL)
public class PasswordStrengthPolicy {
  private boolean enabled;
  private int minNumberOfCharacters;
  private int minNumberOfUppercaseCharacters;
  private int minNumberOfLowercaseCharacters;
  private int minNumberOfSpecialCharacters;
  private int minNumberOfDigits;
}
