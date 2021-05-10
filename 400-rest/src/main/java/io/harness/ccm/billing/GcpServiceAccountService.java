package io.harness.ccm.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.api.services.iam.v1.model.ServiceAccount;
import java.io.IOException;

@OwnedBy(CE)
public interface GcpServiceAccountService {
  ServiceAccount create(String serviceAccountId, String displayName) throws IOException;
  void setIamPolicies(String serviceAccountEmail) throws IOException;
  void addRolesToServiceAccount(String serviceAccountEmail, String[] roles);
}
