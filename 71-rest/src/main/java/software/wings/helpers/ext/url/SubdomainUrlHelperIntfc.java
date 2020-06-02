package software.wings.helpers.ext.url;

import javax.servlet.http.HttpServletRequest;

public interface SubdomainUrlHelperIntfc {
  String getPortalBaseUrl(String accountId);

  String getApiBaseUrl(String accountId);

  String getManagerUrl(HttpServletRequest request, String accountId);

  String getDelegateMetadataUrl(String accountId);

  String getWatcherMetadataUrl(String accountId);

  String getPortalBaseUrlWithoutSeparator(String accountId);
}
