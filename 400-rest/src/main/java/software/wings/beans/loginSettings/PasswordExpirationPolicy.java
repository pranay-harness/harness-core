package software.wings.beans.loginSettings;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class PasswordExpirationPolicy {
  private boolean enabled;
  private int daysBeforePasswordExpires;
  private int daysBeforeUserNotifiedOfPasswordExpiration;
}
