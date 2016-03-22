package software.wings.security;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.Datastore;
import software.wings.app.WingsBootstrap;
import software.wings.beans.AuthToken;
import software.wings.beans.ErrorConstants;
import software.wings.beans.User;
import software.wings.exception.WingsException;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static software.wings.beans.ErrorConstants.INVALID_CREDENTIAL_ERROR_MSG;

/**
 * Created by anubhaw on 3/10/16.
 */
public class BasicAuthAuthenticator implements Authenticator<BasicCredentials, User> {
  Datastore datastore = WingsBootstrap.lookup(Datastore.class);

  @Override
  public Optional<User> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
    User user = datastore.find(User.class).field("email").equal(basicCredentials.getUsername()).get();
    if (null != user && BCrypt.checkpw(basicCredentials.getPassword(), user.getPasswordHash())) {
      AuthToken authToken = new AuthToken(user);
      datastore.save(authToken);
      user.setToken(authToken.getUuid());
      return Optional.of(user);
    }
    throw new WingsException(INVALID_CREDENTIAL_ERROR_MSG);
  }
}
