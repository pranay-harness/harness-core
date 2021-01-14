package io.harness.beans.steps.stepinfo;

import static io.harness.common.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.list;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
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
@JsonTypeName("SaveCacheGCS")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("saveCacheGCSStepInfo")
public class SaveCacheGCSStepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;

  @JsonIgnore
  public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.SAVE_CACHE_GCS).build();

  @JsonIgnore
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(CIStepInfoType.SAVE_CACHE_GCS.name()).build();

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;
  @JsonIgnore @NotNull private ParameterField<String> containerImage;
  private ContainerResource resources;

  // plugin settings
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> key;
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> bucket;
  @NotNull
  @YamlSchemaTypes(value = {list, string}, defaultType = list)
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  private ParameterField<List<String>> sourcePaths;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> target;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "connectorRef", "containerImage", "resources", "key", "bucket",
      "sourcePaths", "target"})
  public SaveCacheGCSStepInfo(String identifier, String name, Integer retry, ParameterField<String> connectorRef,
      ParameterField<String> containerImage, ContainerResource resources, ParameterField<String> key,
      ParameterField<String> bucket, ParameterField<List<String>> sourcePaths, ParameterField<String> target) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.connectorRef = connectorRef;
    this.containerImage = Optional.ofNullable(containerImage)
                              .orElse(ParameterField.createValueField("homerovalle/drone-gcs-cache:latest"));
    if (containerImage != null && containerImage.fetchFinalValue() == null) {
      this.containerImage = ParameterField.createValueField("homerovalle/drone-gcs-cache:latest");
    }
    this.resources = resources;
    this.key = key;
    this.bucket = bucket;
    this.sourcePaths = sourcePaths;
    this.target = target;
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
