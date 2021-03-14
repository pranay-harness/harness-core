package io.harness.ccm.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.google.api.services.cloudresourcemanager.model.Policy;

@TargetModule(Module._490_CE_COMMONS)
public interface GcpResourceManagerService {
  void setPolicy(String projectId, Policy policy);
  Policy getIamPolicy(String projectId);
}
