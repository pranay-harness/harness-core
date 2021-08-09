package software.wings.beans.loginSettings;

import lombok.Builder;
import lombok.Data;

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
