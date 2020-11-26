package io.harness.beans.steps.stepinfo;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.data.validator.EntityIdentifier;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.steps.StepType;

import software.wings.jersey.JsonViews;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import java.beans.ConstructorProperties;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("testIntelligence")
@TypeAlias("testIntelligenceStepInfo")
public class TestIntelligenceStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  // Keeping the timeout to a day as its a test step and might take lot of time
  public static final int DEFAULT_TIMEOUT = 60 * 60 * 24; // 24 hour;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo =
      TypeInfo.builder()
          .stepInfoType(CIStepInfoType.TEST_INTELLIGENCE)
          .stepType(StepType.newBuilder().setType(CIStepInfoType.TEST_INTELLIGENCE.name()).build())
          .build();

  @JsonIgnore private String callbackId;
  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) private int timeout;
  @NotNull private String goals;
  @NotNull private String language;
  @NotNull private String buildTool;

  @NotNull private String image;
  private String connector;
  private ContainerResource resources;
  private int port;

  @Builder
  @ConstructorProperties({"callbackId", "identifier", "name", "retry", "timeout", "goals", "language", "buildTool",
      "image", "connector", "resources", "port"})
  public TestIntelligenceStepInfo(String callbackId, String identifier, String name, int retry, int timeout,
      String goals, String language, String buildTool, String image, String connector, ContainerResource resources,
      int port) {
    this.callbackId = callbackId;
    this.identifier = identifier;
    this.name = name;
    this.retry = retry;
    this.timeout = timeout;
    this.goals = goals;
    this.language = language;
    this.buildTool = buildTool;
    this.image = image;
    this.connector = connector;
    this.resources = resources;
    this.port = port;
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
    return OrchestrationFacilitatorType.ASYNC;
  }
}
