package io.harness.beans.steps.stepinfo;

import static io.harness.common.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_MAP_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;
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
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("BuildAndPushDockerHub")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("dockerStepInfo")
public class DockerStepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;

  @JsonIgnore public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.DOCKER).build();

  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.DOCKER.getDisplayName()).build();

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String identifier;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;
  @JsonIgnore @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> containerImage;
  private ContainerResource resources;

  // plugin settings
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> repo;
  @NotNull
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  private ParameterField<List<String>> tags;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> context;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> dockerfile;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> target;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_MAP_CLASSPATH)
  private ParameterField<Map<String, String>> labels;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_MAP_CLASSPATH)
  private ParameterField<Map<String, String>> buildArgs;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "connectorRef", "containerImage", "resources", "repo", "tags",
      "context", "dockerfile", "target", "labels", "buildArgs"})
  public DockerStepInfo(String identifier, String name, Integer retry, ParameterField<String> connectorRef,
      ParameterField<String> containerImage, ContainerResource resources, ParameterField<String> repo,
      ParameterField<List<String>> tags, ParameterField<String> context, ParameterField<String> dockerfile,
      ParameterField<String> target, ParameterField<Map<String, String>> labels,
      ParameterField<Map<String, String>> buildArgs) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.connectorRef = connectorRef;
    this.containerImage =
        Optional.ofNullable(containerImage).orElse(ParameterField.createValueField("plugins/kaniko:latest"));
    if (containerImage != null && containerImage.fetchFinalValue() == null) {
      this.containerImage = ParameterField.createValueField("plugins/kaniko:latest");
    }

    this.resources = resources;
    this.repo = repo;
    this.tags = tags;
    this.context = context;
    this.dockerfile = dockerfile;
    this.target = target;
    this.labels = labels;
    this.buildArgs = buildArgs;
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
