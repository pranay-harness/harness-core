package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.data.validator.EntityIdentifier;
import io.harness.facilitator.FacilitatorType;
import io.harness.state.StepType;
import io.harness.yaml.core.ExecutionElement;
import lombok.Builder;
import lombok.Data;
import software.wings.jersey.JsonViews;

import java.beans.ConstructorProperties;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@JsonTypeName("liteEngineTask")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiteEngineTaskStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 1200;
  public static final String CALLBACK_IDS = "callbackIds";

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo =
      TypeInfo.builder()
          .stepInfoType(CIStepInfoType.LITE_ENGINE_TASK)
          .stepType(StepType.builder().type(CIStepInfoType.LITE_ENGINE_TASK.name()).build())
          .build();

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) private int timeout;

  @NotNull BuildJobEnvInfo buildJobEnvInfo;
  @NotNull String gitConnectorIdentifier;
  @NotNull String branchName;
  @NotNull boolean usePVC;
  @NotNull String accountId;
  @NotNull ExecutionElement steps;

  @Builder
  @ConstructorProperties({"accountId", "identifier", "name", "retry", "timeout", "buildJobEnvInfo",
      "gitConnectorIdentifier", "branchName", "steps", "usePVC"})
  public LiteEngineTaskStepInfo(String accountId, String identifier, String name, Integer retry, Integer timeout,
      BuildJobEnvInfo buildJobEnvInfo, String gitConnectorIdentifier, String branchName, ExecutionElement steps,
      boolean usePVC) {
    this.accountId = accountId;
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.buildJobEnvInfo = buildJobEnvInfo;
    this.gitConnectorIdentifier = gitConnectorIdentifier;
    this.branchName = branchName;
    this.usePVC = usePVC;
    this.steps = steps;
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
    return FacilitatorType.SYNC;
  }
}
