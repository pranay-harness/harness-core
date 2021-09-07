package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ng.core.account.AuthenticationMechanism;

import software.wings.beans.sso.SSOSettings;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SSOConfig {
  private String accountId;
  private List<SSOSettings> ssoSettings;
  private AuthenticationMechanism authenticationMechanism;
}
