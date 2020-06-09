package io.harness.redesign.advisers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.execution.status.Status.positiveStatuses;

import com.google.common.base.Preconditions;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import software.wings.api.HttpStateExecutionData;

import java.util.Map;

@OwnedBy(CDC)
@Redesign
public class HttpResponseCodeSwitchAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE = AdviserType.builder().type("HTTP_RESPONSE_CODE_SWITCH").build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    if (!positiveStatuses().contains(advisingEvent.getStatus())) {
      return null;
    }
    HttpResponseCodeSwitchAdviserParameters parameters =
        (HttpResponseCodeSwitchAdviserParameters) Preconditions.checkNotNull(advisingEvent.getAdviserParameters());
    // This will be changed to obtain via output type
    HttpStateExecutionData httpStateExecutionData = (HttpStateExecutionData) Preconditions.checkNotNull(
        advisingEvent.getOutcomes()
            .values()
            .stream()
            // TODO => Find a better way to do this
            .filter(outcome -> outcome instanceof HttpStateExecutionData)
            .findFirst()
            .orElse(null));

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
}