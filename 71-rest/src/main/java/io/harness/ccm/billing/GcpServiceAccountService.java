package io.harness.ccm.billing;

import com.google.api.services.iam.v1.model.ServiceAccount;

import java.io.IOException;

public interface GcpServiceAccountService {
  ServiceAccount create(String serviceAccountId, String displayName);
  void setIamPolicies(String serviceAccountEmail) throws IOException;
  void addRoleToServiceAccount(String serviceAccountEmail, String role);
}
