package software.wings.security.authentication;

import lombok.Builder;
import lombok.Data;

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
