package io.harness.redesign.advisers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.StatusUtils;
import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.serializer.KryoSerializer;
import io.harness.state.io.StepOutcomeRef;

import software.wings.api.HttpStateExecutionData;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
public class HttpResponseCodeSwitchAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE = AdviserType.newBuilder().setType("HTTP_RESPONSE_CODE_SWITCH").build();
  @Inject private OutcomeService outcomeService;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    HttpResponseCodeSwitchAdviserParameters parameters =
        (HttpResponseCodeSwitchAdviserParameters) Preconditions.checkNotNull(
            kryoSerializer.asObject(advisingEvent.getAdviserParameters()));
    // This will be changed to obtain via output type
    Outcome outcome = outcomeService.fetchOutcome(advisingEvent.getStepOutcomeRef()
                                                      .stream()
                                                      .filter(oRef -> oRef.getName().equals("http"))
                                                      .findFirst()
                                                      .map(StepOutcomeRef::getInstanceId)
                                                      .orElse(null));

    HttpStateExecutionData httpStateExecutionData = (HttpStateExecutionData) Preconditions.checkNotNull(outcome);

    Map<Integer, String> responseCodeNodeIdMap = parameters.getResponseCodeNodeIdMappings();
    if (responseCodeNodeIdMap.containsKey(httpStateExecutionData.getHttpResponseCode())) {
      return NextStepAdvise.builder()
          .nextNodeId(responseCodeNodeIdMap.get(httpStateExecutionData.getHttpResponseCode()))
          .build();
    } else {
      throw new InvalidRequestException(
          "Not able to process Response For response code: " + httpStateExecutionData.getHttpResponseCode());
    }
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    return StatusUtils.positiveStatuses().contains(advisingEvent.getToStatus());
  }
}
