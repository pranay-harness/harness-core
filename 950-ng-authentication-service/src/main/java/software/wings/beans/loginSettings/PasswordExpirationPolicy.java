package software.wings.beans.loginSettings;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
@OwnedBy(HarnessTeam.PL)
public class PasswordExpirationPolicy {
  private boolean enabled;
  private int daysBeforePasswordExpires;
  private int daysBeforeUserNotifiedOfPasswordExpiration;
}
