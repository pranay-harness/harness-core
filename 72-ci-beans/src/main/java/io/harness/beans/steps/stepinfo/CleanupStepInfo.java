package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.facilitator.FacilitatorType;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Value;
import software.wings.jersey.JsonViews;

import java.beans.ConstructorProperties;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Value
@JsonTypeName("cleanup")
public class CleanupStepInfo implements CIStepInfo, GenericStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 1200;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(CIStepInfoType.CLEANUP)
                                              .stepType(StepType.builder().type(CIStepInfoType.CLEANUP.name()).build())
                                              .build();
  @NotNull String identifier;
  String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) int timeout;

  @NotNull Cleanup cleanup;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "timeout", "cleanup"})
  public CleanupStepInfo(String identifier, String name, Integer retry, Integer timeout, Cleanup cleanup) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.cleanup = cleanup;
  }

  @Value
  @Builder
  public static class Cleanup {}

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }

  @Override
  public StepType getStepType() {
    return typeInfo.getStepType();
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.SYNC;
  }
}
