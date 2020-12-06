package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class TwoFactorAuthenticationSettings {
  private String userId;
  private String email;
  private boolean twoFactorAuthenticationEnabled;
  private TwoFactorAuthenticationMechanism mechanism;
  private String totpSecretKey;
  private String totpqrurl;
}
