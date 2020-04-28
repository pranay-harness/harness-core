package io.harness.redesign.advisers;

import static io.harness.data.structure.CollectionUtils.filterAndGetFirst;

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
import io.harness.state.io.StateTransput;
import software.wings.api.HttpStateExecutionData;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;

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
    HttpStateExecutionData httpStateExecutionData = getOutputByType(StateType.HTTP.name(), stateResponse.getOutputs());
    Map<Integer, String> responseCodeNodeIdMap = parameters.getResponseCodeNodeIdMappings();
    if (httpStateExecutionData == null) {
      throw new InvalidRequestException("State Executed SuccessFully but response data is null ");
    }
    if (responseCodeNodeIdMap.containsKey(httpStateExecutionData.getHttpResponseCode())) {
      return OnSuccessAdvise.builder()
          .nextNodeId(responseCodeNodeIdMap.get(httpStateExecutionData.getHttpResponseCode()))
          .build();
    } else {
      throw new InvalidRequestException(
          "Not able to process Response For response code: " + httpStateExecutionData.getHttpResponseCode());
    }
  }

  private HttpStateExecutionData getOutputByType(@NotNull String type, List<StateTransput> stateOutputs) {
    Optional<StateTransput> outputOptional =
        filterAndGetFirst(stateOutputs, stateOutput -> stateOutput instanceof HttpStateExecutionData);
    return (HttpStateExecutionData) outputOptional.orElse(null);
  }
}