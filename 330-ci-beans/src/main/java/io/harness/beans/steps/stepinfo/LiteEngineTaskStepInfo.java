package io.harness.beans.steps.stepinfo;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.common.SwaggerConstants.INTEGER_CLASSPATH;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.data.validator.EntityIdentifier;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.schema.YamlSchemaIgnoreSubtype;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.beans.ConstructorProperties;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("liteEngineTask")
@JsonIgnoreProperties(ignoreUnknown = true)
@YamlSchemaIgnoreSubtype
@TypeAlias("liteEngineTaskStepInfo")
@OwnedBy(CI)
public class LiteEngineTaskStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 600 * 1000;
  public static final String CALLBACK_IDS = "callbackIds";
  public static final String LOG_KEYS = "logKeys";

  @JsonIgnore
  public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.LITE_ENGINE_TASK).build();
  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.LITE_ENGINE_TASK.getDisplayName()).build();

  @JsonIgnore @Builder.Default int timeout = DEFAULT_TIMEOUT;
  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull BuildJobEnvInfo buildJobEnvInfo;
  @NotNull boolean usePVC;
  @NotNull String accountId;
  @NotNull ExecutionElement steps;
  @NotNull ExecutionElementConfig executionElementConfig;
  CodeBase ciCodebase;
  @NotNull boolean skipGitClone;
  @NotNull Infrastructure infrastructure;
  @JsonIgnore @ApiModelProperty(dataType = INTEGER_CLASSPATH) private ParameterField<Integer> runAsUser;

  @Builder
  @ConstructorProperties({"accountId", "identifier", "name", "retry", "buildJobEnvInfo", "steps",
      "executionElementConfig", "usePVC", "ciCodebase", "skipGitClone", "infrastructure", "runAsUser"})
  public LiteEngineTaskStepInfo(String accountId, String identifier, String name, Integer retry,
      BuildJobEnvInfo buildJobEnvInfo, ExecutionElement steps, ExecutionElementConfig executionElementConfig,
      boolean usePVC, CodeBase ciCodebase, boolean skipGitClone, Infrastructure infrastructure,
      ParameterField<Integer> runAsUser) {
    this.accountId = accountId;
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.buildJobEnvInfo = buildJobEnvInfo;
    this.usePVC = usePVC;
    this.executionElementConfig = executionElementConfig;
    this.steps = steps;
    this.ciCodebase = ciCodebase;
    this.skipGitClone = skipGitClone;
    this.infrastructure = infrastructure;
    this.runAsUser = runAsUser;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
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
