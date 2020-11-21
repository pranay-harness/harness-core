package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.saml.SSORequest;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class LoginTypeResponse {
  private AuthenticationMechanism authenticationMechanism;
  private SSORequest SSORequest;
  private boolean isOauthEnabled;
  private boolean showCaptcha;
}
