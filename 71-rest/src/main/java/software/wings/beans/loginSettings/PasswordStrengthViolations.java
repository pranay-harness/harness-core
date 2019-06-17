package software.wings.beans.loginSettings;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PasswordStrengthViolations {
  private List<PasswordStrengthViolation> passwordStrengthViolationList;
  private boolean enabled;
}
