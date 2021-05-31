package io.harness.beans.steps.stepinfo;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.common.SwaggerConstants.INTEGER_CLASSPATH;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.schema.YamlSchemaIgnoreSubtype;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.beans.ConstructorProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("Cleanup")
@JsonIgnoreProperties(ignoreUnknown = true)
@YamlSchemaIgnoreSubtype
@TypeAlias("CleanupStepInfo")
@OwnedBy(CI)
public class CleanupStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 1200;

  @JsonIgnore public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.CLEANUP).build();
  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.CLEANUP.getDisplayName()).build();
  @JsonIgnore @Builder.Default int timeout = DEFAULT_TIMEOUT;

  @NotNull @EntityIdentifier private String identifier;
  @NotNull Infrastructure infrastructure;
  @NotNull private String podName;
  private String name;
  @JsonIgnore @ApiModelProperty(dataType = INTEGER_CLASSPATH) private ParameterField<Integer> runAsUser;

  @Builder
  @ConstructorProperties({"identifier", "name", "infrastructure", "podName", "runAsUser"})
  public CleanupStepInfo(String identifier, String name, Infrastructure infrastructure, String podName,
      ParameterField<Integer> runAsUser) {
    this.identifier = identifier;
    this.name = name;
    this.infrastructure = infrastructure;
    this.podName = podName;
    this.runAsUser = runAsUser;
  }

  public static CleanupStepInfoBuilder builder() {
    return new CleanupStepInfoBuilder();
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }

  public int getRetry() {
    return 1;
  }

  @Override
  public StepType getStepType() {
    return STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }
}
