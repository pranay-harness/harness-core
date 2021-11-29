package io.harness.cdng.pipeline.executions;

import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.repositories.executions.CDAccountExecutionMetadataRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CDPipelineEndEventHandler implements OrchestrationEventHandler {
  @Inject CDAccountExecutionMetadataRepository cdAccountExecutionMetadataRepository;
  @Override
  public void handleEvent(OrchestrationEvent event) {
    cdAccountExecutionMetadataRepository.updateAccountExecutionMetadata(
        AmbianceUtils.getAccountId(event.getAmbiance()), event.getEndTs());
  }
}
