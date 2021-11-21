package io.harness.gitsync.common.service;

import io.harness.gitsync.common.dtos.TriggerFullSyncRequestDTO;
import io.harness.gitsync.common.dtos.TriggerFullSyncResponseDTO;

public interface FullSyncTriggerService {
  TriggerFullSyncResponseDTO triggerFullSync(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      TriggerFullSyncRequestDTO fullSyncRequest);
}
