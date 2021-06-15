package io.harness.gitsync.core.service.webhookevent;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;

@OwnedBy(HarnessTeam.DX)
public interface GitCreateBranchEventExecutionService {
  void processEvent(WebhookDTO webhookDTO);
}
