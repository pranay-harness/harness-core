package software.wings.security.authentication;

import static org.mindrot.jbcrypt.BCrypt.checkpw;
import static software.wings.beans.ErrorCode.EMAIL_NOT_VERIFIED;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.app.MainConfiguration;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.UserService;

@Singleton
public class PasswordBasedAuthHandler implements AuthHandler {
  private MainConfiguration configuration;

  private UserService userService;

  private WingsPersistence wingsPersistence;

  @Inject
  public PasswordBasedAuthHandler(
      MainConfiguration configuration, UserService userService, WingsPersistence wingsPersistence) {
    this.configuration = configuration;
    this.userService = userService;
    this.wingsPersistence = wingsPersistence;
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

  protected User getUser(String userName) {
    return wingsPersistence.createQuery(User.class).field("email").equal(userName.trim().toLowerCase()).get();
  }
}
