package software.wings.security.authentication;

import static io.harness.eraro.ErrorCode.EMAIL_NOT_VERIFIED;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static org.mindrot.jbcrypt.BCrypt.checkpw;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.User;
import software.wings.service.intfc.UserService;

@Singleton
public class PasswordBasedAuthHandler implements AuthHandler {
  private UserService userService;

  @Inject
  public PasswordBasedAuthHandler(UserService userService) {
    this.userService = userService;
  }

  @Override
  public AuthenticationResponse authenticate(String... credentials) {
    return authenticateInternal(false, credentials);
  }

  private AuthenticationResponse authenticateInternal(boolean isPasswordHash, String... credentials) {
    if (credentials == null || credentials.length != 2) {
      throw new WingsException(INVALID_ARGUMENT);
    }

    String userName = credentials[0];
    String password = credentials[1];

    User user = getUser(userName);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST, USER);
    }
    if (!user.isEmailVerified()) {
      throw new WingsException(EMAIL_NOT_VERIFIED, USER);
    }

    if (isPasswordHash) {
      if (password.equals(user.getPasswordHash())) {
        return new AuthenticationResponse(user);
      }
    } else {
      if (checkpw(password, user.getPasswordHash())) {
        return new AuthenticationResponse(user);
      }
    }

    throw new WingsException(INVALID_CREDENTIAL, USER);
  }

  public AuthenticationResponse authenticateWithPasswordHash(String... credentials) {
    return authenticateInternal(true, credentials);
  }

  @Override
  public AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.USER_PASSWORD;
  }

  protected User getUser(String email) {
    return userService.getUserByEmail(email);
  }
}
