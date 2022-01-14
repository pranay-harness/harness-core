package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.CustomHealthSampleDataRequest;

public interface CustomHealthService {
  Object fetchSampleData(String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier,
      String tracingId, CustomHealthSampleDataRequest request);
}
