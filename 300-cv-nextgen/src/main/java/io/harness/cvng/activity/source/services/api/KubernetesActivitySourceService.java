package io.harness.cvng.activity.source.services.api;

import io.harness.cvng.core.services.api.DataSourceConnectivityChecker;
import io.harness.ng.beans.PageResponse;

public interface KubernetesActivitySourceService extends DataSourceConnectivityChecker {
  int getNumberOfKubernetesServicesSetup(String accountId, String orgIdentifier, String projectIdentifier);
  PageResponse<String> getKubernetesNamespaces(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, int offset, int pageSize, String filter);
  PageResponse<String> getKubernetesWorkloads(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String namespace, int offset, int pageSize, String filter);
}
