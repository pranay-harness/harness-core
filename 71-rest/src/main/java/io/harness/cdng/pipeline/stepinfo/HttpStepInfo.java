package io.harness.cdng.pipeline.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.facilitator.FacilitatorType;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("http")
public class HttpStepInfo implements CDStepInfo {
  String displayName;
  String type;
  String identifier;
  HttpSpec http;

  @Override
  public String getName() {
    return displayName;
  }

  @Override
  public StepType getStepType() {
    return BasicHttpStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.TASK;
  }

  @Value
  @Builder
  public static class HttpSpec {
    String url;
    String body;
    String header;
    String method;
    int socketTimeoutMillis;
  }
}
