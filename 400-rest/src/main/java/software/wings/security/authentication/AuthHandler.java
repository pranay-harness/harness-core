package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ng.core.account.AuthenticationMechanism;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

@OwnedBy(PL)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public interface AuthHandler {
  /***
   * Check if a user's credentials are valid
   * @param credentials
   * @return
   */
  AuthenticationResponse authenticate(String... credentials)
      throws URISyntaxException, InterruptedException, ExecutionException, IOException;

  AuthenticationMechanism getAuthenticationMechanism();
}
