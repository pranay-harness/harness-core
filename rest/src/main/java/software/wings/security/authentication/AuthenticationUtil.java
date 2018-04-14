package software.wings.security.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.http.client.utils.URIBuilder;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.ErrorCode;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@Singleton
public class AuthenticationUtil {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration configuration;

  public User getUser(String userName) {
    return userName != null
        ? wingsPersistence.createQuery(User.class).field("email").equal(userName.trim().toLowerCase()).get()
        : null;
  }

  public Optional<Account> getPrimaryAccount(User user) {
    Optional<Account> account = Optional.empty();
    if (user.getAccounts() != null && user.getAccounts().size() > 0) {
      account = Optional.of(user.getAccounts().get(0));
    }
    return account;
  }

  public URI buildAbsoluteUrl(String path, Map<String, String> params) {
    try {
      String baseUrl = getBaseUrl();
      URIBuilder uriBuilder = new URIBuilder(baseUrl);
      uriBuilder.setPath(path);
      if (params != null) {
        params.entrySet().stream().forEach(entry -> uriBuilder.addParameter(entry.getKey(), entry.getValue()));
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