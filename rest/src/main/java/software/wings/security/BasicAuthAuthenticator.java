package software.wings.security;

import static org.mindrot.jbcrypt.BCrypt.checkpw;
import static software.wings.beans.ErrorCode.EMAIL_NOT_VERIFIED;
import static software.wings.beans.ErrorCode.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.beans.ResponseMessage.Acuteness.HARMLESS;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import software.wings.app.MainConfiguration;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.UserService;

import java.util.Optional;

/**
 * Created by anubhaw on 3/10/16.
 */
@Singleton
public class BasicAuthAuthenticator implements Authenticator<BasicCredentials, User> {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration configuration;
  @Inject private UserService userService;

  /* (non-Javadoc)
   * @see io.dropwizard.auth.Authenticator#authenticate(java.lang.Object)
   */
  @Override
  public Optional<User> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
    User user = wingsPersistence.createQuery(User.class)
                    .field("email")
                    .equal(basicCredentials.getUsername().trim().toLowerCase())
                    .get();
    if (user == null) {
      throw new WingsException(aResponseMessage().code(USER_DOES_NOT_EXIST).acuteness(HARMLESS).build());
    }
    if (!user.isEmailVerified()) {
      throw new WingsException(aResponseMessage().code(EMAIL_NOT_VERIFIED).acuteness(HARMLESS).build());
    }
    if (checkpw(basicCredentials.getPassword(), user.getPasswordHash())) {
      AuthToken authToken = new AuthToken(user.getUuid(), configuration.getPortal().getAuthTokenExpiryInMillis());
      userService.evictUserFromCache(user.getUuid());

      wingsPersistence.save(authToken);
      user.setToken(authToken.getUuid());
      return Optional.of(user);
    }
    throw new WingsException(aResponseMessage().code(INVALID_CREDENTIAL).acuteness(HARMLESS).build());
  }
}
