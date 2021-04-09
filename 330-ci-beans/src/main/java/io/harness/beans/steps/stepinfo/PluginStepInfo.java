package io.harness.beans.steps.stepinfo;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.common.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.common.SwaggerConstants.INTEGER_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_MAP_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIStepInfo;
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
@JsonTypeName("Plugin")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("pluginStepInfo")
@OwnedBy(CI)
public class PluginStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 1;

  @JsonIgnore public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.PLUGIN).build();
  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.PLUGIN.getDisplayName()).build();
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String identifier;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_MAP_CLASSPATH)
  private ParameterField<Map<String, String>> settings;
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> image;
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;
  private ContainerResource resources;

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private List<String> entrypoint;
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH) private ParameterField<Boolean> privileged;
  @JsonIgnore @ApiModelProperty(dataType = INTEGER_CLASSPATH) private ParameterField<Integer> runAsUser;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "settings", "image", "connectorRef", "entrypoint", "resources",
      "privileged", "runAsUser"})
  public PluginStepInfo(String identifier, String name, Integer retry, ParameterField<Map<String, String>> settings,
      ParameterField<String> image, ParameterField<String> connectorRef, List<String> entrypoint,
      ContainerResource resources, ParameterField<Boolean> privileged, ParameterField<Integer> runAsUser) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.settings = settings;
    this.image = image;
    this.connectorRef = connectorRef;
    this.entrypoint = entrypoint;
    this.resources = resources;
    this.privileged = privileged;
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
    return OrchestrationFacilitatorType.ASYNC;
  }
}
