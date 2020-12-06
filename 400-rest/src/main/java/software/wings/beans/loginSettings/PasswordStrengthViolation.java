package software.wings.beans.loginSettings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordStrengthViolation {
  PasswordStrengthChecks passwordStrengthChecks;
  int minimumNumberOfCharacters;
}
