package io.harness.ccm.serviceAccount;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CEGcpServiceAccount;

import java.io.IOException;

@OwnedBy(CE)
public interface CEGcpServiceAccountService {
  CEGcpServiceAccount create(String accountId, String ccmProjectId);
  CEGcpServiceAccount provisionAndRetrieveServiceAccount(String accountId, String ccmProjectId) throws IOException;
}
