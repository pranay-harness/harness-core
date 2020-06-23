package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.data.validator.EntityIdentifier;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.facilitator.FacilitatorType;
import io.harness.state.StepType;
import io.harness.yaml.core.Execution;
import lombok.Builder;
import lombok.Value;
import software.wings.jersey.JsonViews;

import java.beans.ConstructorProperties;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Value
public class LiteEngineTaskStepInfo implements CIStepInfo, GenericStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 1200;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo =
      TypeInfo.builder()
          .stepInfoType(CIStepInfoType.LITE_ENGINE_TASK)
          .stepType(StepType.builder().type(CIStepInfoType.LITE_ENGINE_TASK.name()).build())
          .build();

  @NotNull @EntityIdentifier private String identifier;
  private String displayName;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) private int timeout;

  @NotNull private EnvSetupInfo envSetup;

  @Builder
  @ConstructorProperties({"identifier", "displayName", "retry", "timeout", " envSetup"})
  public LiteEngineTaskStepInfo(
      String identifier, String displayName, Integer retry, Integer timeout, EnvSetupInfo envSetup) {
    this.identifier = identifier;
    this.displayName = displayName;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.envSetup = envSetup;
  }

  @Value
  @Builder
  public static class EnvSetupInfo {
    @NotNull BuildJobEnvInfo buildJobEnvInfo;
    @NotNull String gitConnectorIdentifier;
    @NotNull String branchName;
    @NotNull Execution steps;
  }

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
