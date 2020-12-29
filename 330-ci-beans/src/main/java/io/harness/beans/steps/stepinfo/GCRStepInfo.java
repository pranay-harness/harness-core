package io.harness.beans.steps.stepinfo;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;

import software.wings.jersey.JsonViews;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
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
@JsonTypeName("BuildAndPushGCR")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("gcrStepInfo")
public class GCRStepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.GCR).build();
  @JsonIgnore public static final StepType STEP_TYPE = StepType.newBuilder().setType(CIStepInfoType.GCR.name()).build();

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull private ParameterField<String> connectorRef;
  @JsonIgnore @NotNull private ParameterField<String> image;
  private ContainerResource resources;

  // plugin settings
  @NotNull private ParameterField<String> registry;
  @NotNull private ParameterField<String> repo;
  @NotNull private ParameterField<List<String>> tags;
  private ParameterField<String> context;
  private ParameterField<String> dockerfile;
  private ParameterField<String> target;
  private ParameterField<Map<String, String>> labels;
  private ParameterField<List<String>> buildArgs;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "connectorRef", "image", "resources", "registry", "repo",
      "tags", "context", "dockerfile", "target", "labels", "buildArgs"})
  public GCRStepInfo(String identifier, String name, Integer retry, ParameterField<String> connectorRef,
      ParameterField<String> image, ContainerResource resources, ParameterField<String> registry,
      ParameterField<String> repo, ParameterField<List<String>> tags, ParameterField<String> context,
      ParameterField<String> dockerfile, ParameterField<String> target, ParameterField<Map<String, String>> labels,
      ParameterField<List<String>> buildArgs) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.connectorRef = connectorRef;
    this.image = Optional.ofNullable(image).orElse(ParameterField.createValueField("plugins/kaniko-gcr:latest"));
    this.resources = resources;
    this.registry = registry;
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
