package software.wings.helpers.ext.url;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.servlet.http.HttpServletRequest;

@OwnedBy(PL)
public interface SubdomainUrlHelperIntfc {
  String getPortalBaseUrl(String accountId);

  String getGatewayBaseUrl(String accountId);

  String getPortalBaseUrl(String accountId, String defaultBaseUrl);

  String getApiBaseUrl(String accountId);

  String getManagerUrl(HttpServletRequest request, String accountId);

  String getDelegateMetadataUrl(String accountId, String managerHost, String deployMode);

  String getWatcherMetadataUrl(String accountId, String managerHost, String deployMode);

  String getPortalBaseUrlWithoutSeparator(String accountId);
}
