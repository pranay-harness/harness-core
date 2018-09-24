package software.wings.security.authentication;

import lombok.Builder;
import lombok.Data;
import software.wings.security.saml.SamlRequest;

@Data
@Builder
public class LoginTypeResponse {
  private AuthenticationMechanism authenticationMechanism;
  private SamlRequest samlRequest;
}
