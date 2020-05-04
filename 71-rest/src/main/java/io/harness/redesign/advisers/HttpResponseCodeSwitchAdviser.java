package io.harness.redesign.advisers;

import com.google.common.base.Preconditions;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.impl.success.OnSuccessAdvise;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.exception.InvalidRequestException;
import io.harness.state.io.StateResponse;
import software.wings.api.HttpStateExecutionData;

import java.util.Map;

@Redesign
@Produces(Adviser.class)
public class HttpResponseCodeSwitchAdviser implements Adviser {
  @Override
  public AdviserType getType() {
    return AdviserType.builder().type("HTTP_RESPONSE_CODE_SWITCH").build();
  }

  @Override
  public Advise onAdviseEvent(AdvisingEvent adviseEvent) {
    HttpResponseCodeSwitchAdviserParameters parameters =
        (HttpResponseCodeSwitchAdviserParameters) Preconditions.checkNotNull(adviseEvent.getAdviserParameters());
    StateResponse stateResponse = adviseEvent.getStateResponse();
    // This will be changed to obtain via output type
    HttpStateExecutionData httpStateExecutionData = (HttpStateExecutionData) Preconditions.checkNotNull(
        stateResponse.getOutcomes()
            .stream()
            .filter(outcome -> HttpStateExecutionData.OUTCOME_TYPE == outcome.getOutcomeType())
            .findFirst()
            .orElse(null));

    Map<Integer, String> responseCodeNodeIdMap = parameters.getResponseCodeNodeIdMappings();
    if (responseCodeNodeIdMap.containsKey(httpStateExecutionData.getHttpResponseCode())) {
      return OnSuccessAdvise.builder()
          .nextNodeId(responseCodeNodeIdMap.get(httpStateExecutionData.getHttpResponseCode()))
          .build();
    } else {
      throw new InvalidRequestException(
          "Not able to process Response For response code: " + httpStateExecutionData.getHttpResponseCode());
    }
  }
}