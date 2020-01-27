package software.wings.helpers.ext.url;

import java.util.Optional;

public interface SubdomainUrlHelperIntfc {
  String getPortalBaseUrl(Optional<String> subdomainUrl);

  String getApiBaseUrl(Optional<String> subdomainUrl);

  Optional<String> getCustomSubDomainUrl(Optional<String> accountId);

  String getAPIUrl();
}
