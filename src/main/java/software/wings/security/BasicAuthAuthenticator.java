package software.wings.security;

import static software.wings.beans.ErrorConstants.INVALID_CREDENTIAL_ERROR_MSG;

import org.mindrot.jbcrypt.BCrypt;

import com.google.common.base.Optional;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import software.wings.app.WingsBootstrap;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;

/**
 * Created by anubhaw on 3/10/16.
 */
public class BasicAuthAuthenticator implements Authenticator<BasicCredentials, User> {
  WingsPersistence wingsPersistence = WingsBootstrap.lookup(WingsPersistence.class);

  @Override
  public Optional<User> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
    User user = wingsPersistence.createQuery(User.class).field("email").equal(basicCredentials.getUsername()).get();
    if (null != user && BCrypt.checkpw(basicCredentials.getPassword(), user.getPasswordHash())) {
      AuthToken authToken = new AuthToken(user);
      wingsPersistence.save(authToken);
      user.setToken(authToken.getUuid());
      return Optional.of(user);
    }
    throw new WingsException(INVALID_CREDENTIAL_ERROR_MSG);
  }
}
