package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.CustomHealthSampleDataRequest;

import java.util.Map;

public interface CustomHealthService {
  Map<String, Object> fetchSampleData(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String tracingId, CustomHealthSampleDataRequest request);
}
