package io.harness.ccm.serviceAccount;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.api.services.cloudresourcemanager.model.Policy;

@OwnedBy(CE)
public interface GcpResourceManagerService {
  void setPolicy(String projectId, Policy policy);
  Policy getIamPolicy(String projectId);
}
