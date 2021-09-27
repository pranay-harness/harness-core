package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.execution.events.AdviserResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.execution.utils.SdkResponseEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class AdviserResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private OrchestrationEngine orchestrationEngine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    AdviserResponseRequest request = event.getAdviserResponseRequest();
    orchestrationEngine.handleAdvise(SdkResponseEventUtils.getNodeExecutionId(event), request.getAdviserResponse());
  }
}
