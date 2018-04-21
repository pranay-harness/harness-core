package software.wings.security.authentication;

import software.wings.beans.User;

public interface TwoFactorAuthHandler {
  /***
   * Check if a user's credentials are valid . The user should already have been authenticated once by the primary
   * authentication mechanism
   * @param params
   * @return
   */
  User authenticate(User user, String... credentials);

  TwoFactorAuthenticationMechanism getAuthenticationMechanism();

  TwoFactorAuthenticationSettings createTwoFactorAuthenticationSettings(User user);

  User applyTwoFactorAuthenticationSettings(User user, TwoFactorAuthenticationSettings settings);

  User disableTwoFactorAuthentication(User user);

  boolean resetAndSendEmail(User user);
}
