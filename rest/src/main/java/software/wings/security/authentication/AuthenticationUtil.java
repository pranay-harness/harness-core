package software.wings.security.authentication;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.http.client.utils.URIBuilder;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.ErrorCode;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.service.intfc.UserService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Singleton
public class AuthenticationUtil {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;
  @Inject private MainConfiguration configuration;

  public User getUser(String userName) {
    return getUser(userName, null);
  }

  public User getUser(String userName, ReportTarget[] reportTargets) {
    User user = isNotEmpty(userName) ? getUserByEmail(userName) : null;
    if (user == null) {
      if (reportTargets == null) {
        throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
      } else {
        throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST, reportTargets);
      }
    }
    return user;
  }

  protected User getUserByEmail(String userName) {
    return wingsPersistence.createQuery(User.class).field("email").equal(userName.trim().toLowerCase()).get();
  }

  public Account getPrimaryAccount(User user) {
    return user.getAccounts().get(0);
  }

  public URI buildAbsoluteUrl(String path, Map<String, String> params) {
    try {
      String baseUrl = getBaseUrl();
      URIBuilder uriBuilder = new URIBuilder(baseUrl);
      uriBuilder.setPath(path);
      if (params != null) {
        params.forEach((name, value) -> uriBuilder.addParameter(name, value));
      }
      return uriBuilder.build();
    } catch (URISyntaxException e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }
  }

  public String getBaseUrl() {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }
}