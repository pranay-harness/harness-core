package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.data.validator.EntityIdentifier;
import io.harness.facilitator.FacilitatorType;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Data;
import software.wings.jersey.JsonViews;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@JsonTypeName("run")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 1200;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(CIStepInfoType.RUN)
                                              .stepType(StepType.builder().type(CIStepInfoType.RUN.name()).build())
                                              .build();

  @JsonIgnore private String callbackId;
  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) private int timeout;
  @NotNull private List<String> command;
  private List<String> envVariables;
  private String envVarsOutput;
  private List<String> output;
  @Builder

  @ConstructorProperties(
      {"callbackId", "identifier", "name", "retry", "timeout", "command", "envVariables", "envVarsOutput", "output"})
  public RunStepInfo(String callbackId, String identifier, String name, Integer retry, Integer timeout,
      List<String> command, List<String> envVariables, String envVarsOutput, List<String> output) {
    this.callbackId = callbackId;
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.command = command;
    this.envVariables = envVariables;
    this.envVarsOutput = envVarsOutput;
    this.output = output;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return typeInfo.getStepType();
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.ASYNC;
  }
}
