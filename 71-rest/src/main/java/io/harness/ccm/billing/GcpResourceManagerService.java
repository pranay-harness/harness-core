package io.harness.ccm.billing;

import com.google.api.services.cloudresourcemanager.model.Policy;

public interface GcpResourceManagerService {
  void setPolicy(String projectId, Policy policy);
  Policy getIamPolicy(String projectId);
}
