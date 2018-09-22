package software.wings.security.authentication;

import software.wings.beans.User;

public interface AuthHandler {
  /***
   * Check if a user's credentials are valid
   * @param params
   * @return
   */
  User authenticate(String... credentials);

  AuthenticationMechanism getAuthenticationMechanism();
}
