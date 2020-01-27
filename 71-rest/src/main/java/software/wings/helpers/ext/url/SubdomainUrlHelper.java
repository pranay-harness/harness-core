package software.wings.helpers.ext.url;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.UrlConfiguration;
import software.wings.beans.Account;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;

import java.util.List;
import java.util.Optional;

/**
 * Created by mehulkasliwal on 2020-01-21.
 */
@Singleton
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class SubdomainUrlHelper implements SubdomainUrlHelperIntfc {
  @Inject private AccountService accountService;
  @Inject private UrlConfiguration urlConfiguration;

  /**
   * Returns Base URL required for sending emails
   * @param subdomainUrl Optional Type object containing subdomain URL
   * @return Base URl
   */
  @Override
  public String getPortalBaseUrl(Optional<String> subdomainUrl) {
    // Set baseUrl = subDomainUrl only if subDomainUrl is not null, otherwise
    // set baseUrl equal to URL of portal
    return subdomainUrl.isPresent() ? subdomainUrl.get() : getPortalUrl();
  }

  /**
   * Returns Base URL required for sending slack notifications
   * @param subdomainUrl Optional Type object containing subdomain URL
   * @return Base URl
   */
  @Override
  public String getApiBaseUrl(Optional<String> subdomainUrl) {
    // Set baseUrl = subDomainUrl only if subDomainUrl is not null, otherwise
    // set baseUrl equal to API URL
    return subdomainUrl.isPresent() ? subdomainUrl.get() : getAPIUrl();
  }

  /**
   * Returns Base URL of the portal
   * @return
   */
  private String getPortalUrl() {
    String baseUrl = urlConfiguration.getPortalUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }

  /**
   * Returns Base URL of API
   * @return
   */
  public String getAPIUrl() {
    String baseUrl = urlConfiguration.getApiUrl();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }

  public Optional<String> getCustomSubDomainUrl(Optional<String> accountId) {
    try {
      if (accountId.isPresent()) {
        String defaultAccountId = accountId.get();
        Account account = accountService.get(defaultAccountId);
        return Optional.ofNullable(account.getSubdomainUrl());
      } else {
        String defaultAccountId = UserThreadLocal.get().getUserRequestContext().getAccountId();
        List<Account> accounts = UserThreadLocal.get().getAccounts();
        String subdomainUrl = null;
        if (!accounts.isEmpty()) {
          Optional<Account> account = accounts.stream().filter(p -> p.getUuid().equals(defaultAccountId)).findFirst();
          subdomainUrl = account.isPresent() ? account.get().getSubdomainUrl() : null;
        }
        return Optional.ofNullable(subdomainUrl);
      }
    } catch (Exception e) {
      logger.info("Exception occurred at getCustomSubdomainUrl", e);
      return Optional.ofNullable(null);
    }
  }
}
