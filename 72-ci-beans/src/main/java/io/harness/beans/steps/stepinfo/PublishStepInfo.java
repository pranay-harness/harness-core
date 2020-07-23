package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.Artifact;
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
@JsonTypeName("publishArtifacts")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 1200;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(CIStepInfoType.PUBLISH)
                                              .stepType(StepType.builder().type(CIStepInfoType.PUBLISH.name()).build())
                                              .build();

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) private int timeout;

  @NotNull private List<Artifact> publishArtifacts;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "timeout", "publishArtifacts"})
  PublishStepInfo(String identifier, String name, Integer retry, Integer timeout, List<Artifact> publishArtifacts) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.publishArtifacts = publishArtifacts;
  }

  public static PublishStepInfoBuilder builder() {
    return new PublishStepInfoBuilder();
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
