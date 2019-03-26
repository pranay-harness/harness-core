package software.wings.security.authentication;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

public interface AuthHandler {
  /***
   * Check if a user's credentials are valid
   * @param params
   * @return
   */
  AuthenticationResponse authenticate(String... credentials)
      throws URISyntaxException, InterruptedException, ExecutionException, IOException;

  AuthenticationMechanism getAuthenticationMechanism();
}
