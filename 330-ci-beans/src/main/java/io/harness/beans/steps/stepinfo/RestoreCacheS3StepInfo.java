package io.harness.beans.steps.stepinfo;

import static io.harness.common.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_CLASSPATH;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.ArchiveFormat;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;

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
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("RestoreCacheS3")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("restoreCacheS3StepInfo")
public class RestoreCacheS3StepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;

  @JsonIgnore
  public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.RESTORE_CACHE_S3).build();

  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.RESTORE_CACHE_S3.getDisplayName()).build();

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String identifier;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;
  private ContainerResource resources;

  // plugin settings
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> key;
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> bucket;

  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> region;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> endpoint;
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH) private ParameterField<Boolean> pathStyle;
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH) private ParameterField<Boolean> failIfKeyNotFound;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<ArchiveFormat> archiveFormat;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "connectorRef", "resources", "key", "bucket", "region",
      "endpoint", "pathStyle", "failIfKeyNotFound", "archiveFormat"})
  public RestoreCacheS3StepInfo(String identifier, String name, Integer retry, ParameterField<String> connectorRef,
      ContainerResource resources, ParameterField<String> key, ParameterField<String> bucket,
      ParameterField<String> region, ParameterField<String> endpoint, ParameterField<Boolean> pathStyle,
      ParameterField<Boolean> failIfKeyNotFound, ParameterField<ArchiveFormat> archiveFormat) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.connectorRef = connectorRef;
    this.resources = resources;
    this.key = key;
    this.bucket = bucket;
    this.region = region;
    this.endpoint = endpoint;
    this.pathStyle = pathStyle;
    this.failIfKeyNotFound = failIfKeyNotFound;
    this.archiveFormat = archiveFormat;
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
