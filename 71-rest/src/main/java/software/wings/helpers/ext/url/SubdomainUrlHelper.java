package software.wings.helpers.ext.url;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.app.UrlConfiguration;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AccountThreadLocal;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FeatureFlagService;

import java.net.URL;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

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
  @Inject private FeatureFlagService featureFlagService;

  /**
   * Returns Portal URL
   * @param accountId
   * @return Base URl
   */
  @Override
  public String getPortalBaseUrl(String accountId) {
    // Set baseUrl = subDomainUrl only if subDomainUrl is not null, otherwise
    // set baseUrl equal to URL of portal
    logger.info("Generating Portal URL for account {}", accountId);
    String portalUrl = appendSeparatorToUrl(getPortalUrl(accountId));
    logger.info("Returning {} from getPortalBaseUrl", portalUrl);
    return portalUrl;
  }

  private String getPortalUrl(String accountId) {
    Optional<String> subdomainUrl = getCustomSubdomainUrl(accountId);
    return subdomainUrl.isPresent() ? subdomainUrl.get() : urlConfiguration.getPortalUrl().trim();
  }

  /**
   * Returns API URL
   * @param accountId
   * @return Base URL
   */
  @Override
  public String getApiBaseUrl(String accountId) {
    // Set baseUrl = subDomainUrl only if subDomainUrl is not null, otherwise
    // set baseUrl equal to API URL
    logger.info("Generating API URL for account {}", accountId);
    Optional<String> subdomainUrl = getCustomSubdomainUrl(accountId);
    String apiUrl = subdomainUrl.isPresent() ? subdomainUrl.get() : urlConfiguration.getApiUrl().trim();
    logger.info("Returning {} from getApiBaseUrl", apiUrl);
    return appendSeparatorToUrl(apiUrl);
  }

  /**
   * Returns an optional object containing subdomain URL of the account
   * @param accountId
   * @return
   */
  private Optional<String> getCustomSubdomainUrl(String accountId) {
    logger.info("Getting subdomainUrl for account Id: {}", accountId);
    try {
      if (null == accountId) {
        accountId = getAccountIdFromThreadLocal();
      }
      if (null != accountId) {
        Account account = accountService.get(accountId);
        logger.info("Returning subdomain URL {} for account {}", account.getSubdomainUrl(), accountId);
        return Optional.ofNullable(account.getSubdomainUrl());
      }
    } catch (Exception e) {
      logger.info("Exception occurred at getCustomSubdomainUrl for account {}", accountId, e);
      return Optional.empty();
    }
    return Optional.empty();
  }

  private String getAccountIdFromThreadLocal() {
    String accountId = AccountThreadLocal.get();
    logger.info("Got account id {} from AccountThreadLocal", accountId);

    if (null == accountId) {
      User user = UserThreadLocal.get();
      if (null != user && null != user.getUserRequestContext()) {
        accountId = user.getUserRequestContext().getAccountId();
        logger.info("Got account id {} from UserThreadLocal", accountId);
      }
    }
    return accountId;
  }

  /**
   * Appends / at the end of URL
   * @param url
   * @return
   */
  private String appendSeparatorToUrl(String url) {
    if (url != null && !url.endsWith("/")) {
      url += "/";
    }
    return url;
  }

  /**
   * Removes / at the end of URL
   * @param url
   * @return
   */
  private String removeSeparatorFromUrl(String url) {
    if (url != null && url.endsWith("/")) {
      url = StringUtils.chop(url);
    }
    return url;
  }

  /**
   * Returns manager Url used for Delegate
   * @param request
   * @return
   */
  public String getManagerUrl(HttpServletRequest request, String accountId) {
    logger.info("Generating manager URL for account {}", accountId);
    Optional<String> subdomainUrl = getCustomSubdomainUrl(accountId);
    String apiUrl = subdomainUrl.isPresent() ? subdomainUrl.get() : urlConfiguration.getApiUrl().trim();
    apiUrl = removeSeparatorFromUrl(apiUrl);
    logger.info("Returning manager URL {} for account {}", apiUrl, accountId);
    return !StringUtils.isEmpty(apiUrl)
        ? apiUrl
        : request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }

  public String getDelegateMetadataUrl(String accountId) {
    logger.info("Generating delegate metadata URL for account {}", accountId);
    Optional<String> subdomainUrl = getCustomSubdomainUrl(accountId);
    String delegateMetadataUrl = urlConfiguration.getDelegateMetadataUrl().trim();
    delegateMetadataUrl =
        overrideMetadataUrl(subdomainUrl.isPresent() ? subdomainUrl.get() : null, delegateMetadataUrl);
    logger.info("Returning delegate metadata URL {} for account {}", delegateMetadataUrl, accountId);
    return delegateMetadataUrl;
  }

  public String getWatcherMetadataUrl(String accountId) {
    logger.info("Generating watcher metadata URL for account {}", accountId);
    Optional<String> subdomainUrl = getCustomSubdomainUrl(accountId);
    String watcherMetadataUrl = urlConfiguration.getWatcherMetadataUrl().trim();
    watcherMetadataUrl = overrideMetadataUrl(subdomainUrl.isPresent() ? subdomainUrl.get() : null, watcherMetadataUrl);
    logger.info("Returning watcher metadata URL {} for account {}", watcherMetadataUrl, accountId);
    return watcherMetadataUrl;
  }

  /**
   * Replaces metadata URL according to vanity URL
   * @param subdomainUrl
   * @param metadataUrl
   * @return
   */
  private String overrideMetadataUrl(String subdomainUrl, String metadataUrl) {
    try {
      if (StringUtils.isNotEmpty(subdomainUrl) && StringUtils.isNotEmpty(metadataUrl)) {
        URL originalMetadataUrl = new URL(metadataUrl);
        URL customSubdomainUrl = new URL(subdomainUrl);
        URL modifiedMetadataUrl =
            new URL(customSubdomainUrl.getProtocol(), customSubdomainUrl.getHost(), originalMetadataUrl.getFile());
        logger.info("Returning {} from replaceMetadataUrl", modifiedMetadataUrl.toString());
        return modifiedMetadataUrl.toString();
      }
    } catch (Exception e) {
      logger.info("Exception occurred at replaceMetaDataUrl with metadataUrl {}", metadataUrl, e);
      return metadataUrl;
    }
    return metadataUrl;
  }

  /**
   * Returns Portal URL according to the VANITY_URL feature flag
   * @param accountId
   * @return
   */
  public String getPortalBaseUrlWithoutSeparator(String accountId) {
    logger.info("Getting download URL for account {}", accountId);
    String portalUrl = removeSeparatorFromUrl(getPortalUrl(accountId));
    logger.info("Returning {} as download URL for account {}", portalUrl, accountId);
    return portalUrl;
  }
}
