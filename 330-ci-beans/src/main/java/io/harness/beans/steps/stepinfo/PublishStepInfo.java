package io.harness.beans.steps.stepinfo;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.Artifact;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("publishStepInfo")
public class PublishStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;

  @JsonIgnore public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.PUBLISH).build();
  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.PUBLISH.getDisplayName()).build();

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull private List<Artifact> publishArtifacts;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "publishArtifacts"})
  PublishStepInfo(String identifier, String name, Integer retry, List<Artifact> publishArtifacts) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.publishArtifacts = publishArtifacts;
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
    return STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }
}
