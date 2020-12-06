package software.wings.beans.loginSettings;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordStrengthViolations {
  private List<PasswordStrengthViolation> passwordStrengthViolationList;
  private boolean enabled;
}
