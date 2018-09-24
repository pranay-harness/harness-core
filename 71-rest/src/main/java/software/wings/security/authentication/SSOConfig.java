package software.wings.security.authentication;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.sso.SSOSettings;

import java.util.List;

@Data
@Builder
public class SSOConfig {
  private String accountId;
  private List<SSOSettings> ssoSettings;
  private AuthenticationMechanism authenticationMechanism;
}
