package software.wings.security.authentication;

import lombok.Builder;
import lombok.Data;
import software.wings.security.saml.SSORequest;

@Data
@Builder
public class LoginTypeResponse {
  private AuthenticationMechanism authenticationMechanism;
  private SSORequest SSORequest;
  private boolean isOauthEnabled;
  private boolean showCaptcha;
}
