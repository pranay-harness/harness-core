package software.wings.security.authentication;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.USER_DISABLED;
import static io.harness.exception.WingsException.USER;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

@Singleton
@Slf4j
public class AuthenticationUtils {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;
  @Inject private MainConfiguration configuration;
  @Inject private AccountService accountService;
  @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;

  public User getUserOrReturnNullIfUserDoesNotExists(String userName) {
    if (Strings.isNullOrEmpty(userName)) {
      return null;
    }
    return getUserByEmail(userName);
  }

  public User getUser(String userName) {
    return getUser(userName, null);
  }

  public User getUser(String userName, EnumSet<ReportTarget> reportTargets) {
    User user = isNotEmpty(userName) ? getUserByEmail(userName) : null;
    if (user == null) {
      if (reportTargets == null) {
        throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
      } else {
        throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST, reportTargets);
      }
    } else if (user.isDisabled()) {
      logger.info("User {} is disabled.", userName);
      throw new WingsException(USER_DISABLED, USER);
    }
    return user;
  }

  protected User getUserByEmail(String userName) {
    return wingsPersistence.createQuery(User.class).field("email").equal(userName.trim().toLowerCase()).get();
  }

  public Account getDefaultAccount(User user) {
    String defaultAccountId = user.getDefaultAccountId();
    if (isEmpty(defaultAccountId)) {
      Preconditions.checkNotNull(user.getAccounts(), "Account field in user is null.");
      return user.getAccounts().get(0);
    } else {
      return accountService.get(defaultAccountId);
    }
  }

  public URI buildAbsoluteUrl(String baseUrl, String path, Map<String, String> params) {
    return buildAbsoluteUrlInternal(baseUrl, path, params);
  }

  public URI buildAbsoluteUrl(String path, Map<String, String> params, String accountId) {
    String baseUrl = getBaseUrl(accountId);
    return buildAbsoluteUrlInternal(baseUrl, path, params);
  }

  private URI buildAbsoluteUrlInternal(String baseUrl, String path, Map<String, String> params) {
    try {
      URIBuilder uriBuilder = new URIBuilder(baseUrl);
      uriBuilder.setPath(path);
      if (params != null) {
        params.forEach(uriBuilder::addParameter);
      }
      return uriBuilder.build();
    } catch (URISyntaxException e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }
  }

  public String getBaseUrl() {
    return subdomainUrlHelper.getAPIUrl();
  }

  public String getBaseUrl(String accountId) {
    Optional<String> subdomainUrl = subdomainUrlHelper.getCustomSubDomainUrl(Optional.ofNullable(accountId));
    return subdomainUrlHelper.getApiBaseUrl(subdomainUrl);
  }
}