package io.harness.ccm.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.google.api.services.iam.v1.model.ServiceAccount;
import java.io.IOException;
@TargetModule(Module._490_CE_COMMONS)
public interface GcpServiceAccountService {
  ServiceAccount create(String serviceAccountId, String displayName) throws IOException;
  void setIamPolicies(String serviceAccountEmail) throws IOException;
  void addRolesToServiceAccount(String serviceAccountEmail, String[] roles);
}
