package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.CVSetupStatusDTO;

public interface CVSetupService {
  CVSetupStatusDTO getSetupStatus(String accountId, String orgIdentifier, String projectIdentifier);
}
