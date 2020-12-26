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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("Run")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("runStepInfo")
public class RunStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 1;

  @JsonIgnore public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.RUN).build();
  @JsonIgnore public static final StepType STEP_TYPE = StepType.newBuilder().setType(CIStepInfoType.RUN.name()).build();

  @JsonIgnore private String callbackId;
  @JsonIgnore private Integer port;
  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull private String command;
  private List<String> output;
  private Map<String, String> environment;
  private String skipCondition;
  private List<UnitTestReport> reports;

  @NotNull private String image;
  private String connector;
  private ContainerResource resources;

  @Builder
  @ConstructorProperties({"callbackId", "port", "identifier", "name", "retry", "command", "output", "skipCondition",
      "reports", "environment", "image", "connector", "resources"})
  public RunStepInfo(String callbackId, Integer port, String identifier, String name, Integer retry, String command,
      List<String> output, String skipCondition, List<UnitTestReport> reports, Map<String, String> environment,
      String image, String connector, ContainerResource resources) {
    this.callbackId = callbackId;
    this.port = port;
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.command = command;
    this.environment = environment;
    this.skipCondition = skipCondition;
    this.reports = reports;
    this.output = output;
    this.image = image;
    this.connector = connector;
    this.resources = resources;
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
