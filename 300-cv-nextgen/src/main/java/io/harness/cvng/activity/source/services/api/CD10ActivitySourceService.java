package io.harness.cvng.activity.source.services.api;

import io.harness.cvng.beans.activity.ActivitySourceDTO;

public interface CD10ActivitySourceService {
  ActivitySourceDTO get(String accountId, String projectIdentifier, String appId);
  ActivitySourceDTO get(String accountId, String projectIdentifier, String appId, String envId, String serviceId);
}
