package software.wings.security.authentication;

import static org.mindrot.jbcrypt.BCrypt.checkpw;
import static software.wings.beans.ErrorCode.EMAIL_NOT_VERIFIED;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.app.MainConfiguration;
import software.wings.beans.User;
import software.wings.exception.WingsException;
import software.wings.service.intfc.UserService;

@Singleton
public class PasswordBasedAuthHandler implements AuthHandler {
  private MainConfiguration configuration;
  private UserService userService;

  @SuppressFBWarnings("URF_UNREAD_FIELD")
  @Inject
  public PasswordBasedAuthHandler(MainConfiguration configuration, UserService userService) {
    this.configuration = configuration;
    this.userService = userService;
  }

  @Override
  public User authenticate(String... credentials) {
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
    if (checkpw(password, user.getPasswordHash())) {
      return user;
    }
    throw new WingsException(INVALID_CREDENTIAL, USER);
  }

  @Override
  public AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.USER_PASSWORD;
  }

  protected User getUser(String email) {
    return userService.getUserByEmail(email);
  }
}
