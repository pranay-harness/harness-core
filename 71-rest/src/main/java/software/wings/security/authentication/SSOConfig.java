package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.sso.SSOSettings;

import java.util.List;

@OwnedBy(PL)
@Data
@Builder
public class SSOConfig {
  private String accountId;
  private List<SSOSettings> ssoSettings;
  private AuthenticationMechanism authenticationMechanism;
}
