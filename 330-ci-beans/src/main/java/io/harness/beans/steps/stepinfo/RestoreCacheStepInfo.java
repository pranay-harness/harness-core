package io.harness.beans.steps.stepinfo;

import static io.harness.common.SwaggerConstants.STRING_CLASSPATH;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("restoreCacheStepInfo")
public class RestoreCacheStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  @JsonIgnore
  public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.RESTORE_CACHE).build();

  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.RESTORE_CACHE.getDisplayName()).build();

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> key;
  private ParameterField<Boolean> failIfNotExist;

  @Builder
  @ConstructorProperties({"name", "retry", "key", "failIfNotExist"})
  public RestoreCacheStepInfo(String identifier, String name, Integer retry, ParameterField<String> key,
      ParameterField<Boolean> failIfNotExist) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.key = key;
    this.failIfNotExist = failIfNotExist;
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
