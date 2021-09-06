package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;

@OwnedBy(PL)
@Data
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class LogoutResponse {
  private String logoutUrl;
}
