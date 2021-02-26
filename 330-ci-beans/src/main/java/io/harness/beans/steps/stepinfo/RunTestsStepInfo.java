package io.harness.beans.steps.stepinfo;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.beans.yaml.extended.reports.UnitTestReport;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.beans.ConstructorProperties;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("RunTests")
@TypeAlias("runTestsStepInfo")
public class RunTestsStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  // Keeping the timeout to a day as its a test step and might take lot of time
  public static final int DEFAULT_TIMEOUT = 60 * 60 * 24; // 24 hour;

  @JsonIgnore public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.RUN_TESTS).build();

  @JsonIgnore
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(CIStepInfoType.RUN_TESTS.name()).build();

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull private String args;
  @NotNull private String language;
  @NotNull private String buildTool;
  @NotNull private String packages;
  private String testAnnotations;
  private UnitTestReport reports;
  private boolean runOnlySelectedTests;

  @NotNull private String image;
  private String connector;
  private ContainerResource resources;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "args", "language", "buildTool", "image", "connector",
      "resources", "reports", "testAnnotations", "packages", "runOnlySelectedTests"})
  public RunTestsStepInfo(String identifier, String name, Integer retry, String args, String language, String buildTool,
      String image, String connector, ContainerResource resources, UnitTestReport reports, String testAnnotations,
      String packages, boolean runOnlySelectedTests) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.args = args;
    this.language = language;
    this.buildTool = buildTool;
    this.image = image;
    this.connector = connector;
    this.resources = resources;
    this.reports = reports;
    this.testAnnotations = testAnnotations;
    this.packages = packages;
    this.runOnlySelectedTests = runOnlySelectedTests;
  }

  @Override
  public long getDefaultTimeout() {
    return DEFAULT_TIMEOUT;
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
