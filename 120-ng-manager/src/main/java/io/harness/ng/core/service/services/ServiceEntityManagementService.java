package io.harness.ng.core.service.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ServiceEntityManagementService {
  boolean deleteService(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String ifMatch);
}
