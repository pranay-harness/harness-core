package software.wings.beans.loginSettings;

import io.harness.annotation.HarnessExportableEntity;
import lombok.Builder;
import lombok.Data;

@HarnessExportableEntity
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
