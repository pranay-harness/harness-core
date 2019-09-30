package software.wings.beans.loginSettings;

import io.harness.annotation.HarnessEntity;
import lombok.Builder;
import lombok.Data;

@HarnessEntity(exportable = true)
@Data
@Builder
public class PasswordStrengthPolicy {
  private boolean enabled;
  private int minNumberOfCharacters;
  private int minNumberOfUppercaseCharacters;
  private int minNumberOfLowercaseCharacters;
  private int minNumberOfSpecialCharacters;
  private int minNumberOfDigits;
}
