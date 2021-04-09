package io.harness.beans.steps.stepinfo;

import static io.harness.common.SwaggerConstants.INTEGER_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_CLASSPATH;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
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
@JsonTypeName("S3Upload")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("uploadToS3StepInfo")
public class UploadToS3StepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;

  @JsonIgnore public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.UPLOAD_S3).build();
  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.UPLOAD_S3.getDisplayName()).build();

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String identifier;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;
  private ContainerResource resources;
  @JsonIgnore @ApiModelProperty(dataType = INTEGER_CLASSPATH) private ParameterField<Integer> runAsUser;

  // plugin settings
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> endpoint;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> region;
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> bucket;
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> sourcePath;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> target;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "connectorRef", "resources", "endpoint", "region", "bucket",
      "sourcePath", "target", "runAsUser"})
  public UploadToS3StepInfo(String identifier, String name, Integer retry, ParameterField<String> connectorRef,
      ContainerResource resources, ParameterField<String> endpoint, ParameterField<String> region,
      ParameterField<String> bucket, ParameterField<String> sourcePath, ParameterField<String> target,
      ParameterField<Integer> runAsUser) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.connectorRef = connectorRef;
    this.resources = resources;
    this.endpoint = endpoint;
    this.region = region;
    this.bucket = bucket;
    this.sourcePath = sourcePath;
    this.target = target;
    this.runAsUser = runAsUser;
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
