package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.graph.stepDetail.service.PmsGraphStepDetailsService;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.data.stepdetails.PmsStepDetails;
import io.harness.pms.execution.utils.SdkResponseEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class AddStepDetailsInstanceRequestProcessor implements SdkResponseProcessor {
  @Inject private PmsGraphStepDetailsService graphStepDetailsService;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    PmsStepDetails stepDetail = PmsStepDetails.parse(event.getStepDetailsInstanceRequest().getStepDetails());
    graphStepDetailsService.addStepDetail(SdkResponseEventUtils.getNodeExecutionId(event),
        SdkResponseEventUtils.getPlanExecutionId(event), stepDetail, event.getStepDetailsInstanceRequest().getName());
  }
}