package software.wings.beans.loginSettings;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
public class PasswordExpirationPolicy {
  private boolean enabled;
  private int daysBeforePasswordExpires;
  private int daysBeforeUserNotifiedOfPasswordExpiration;
}
