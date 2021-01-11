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
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("RestoreCacheS3")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("restoreCacheS3StepInfo")
public class RestoreCacheS3StepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.RESTORE_CACHE_S3).build();

  @JsonIgnore
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CIStepInfoType.RESTORE_CACHE_S3.name()).build();

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull private ParameterField<String> connectorRef;
  @JsonIgnore @NotNull private ParameterField<String> containerImage;
  private ContainerResource resources;

  // plugin settings
  private ParameterField<String> endpoint;
  @NotNull private ParameterField<String> key;
  @NotNull private ParameterField<String> bucket;
  private ParameterField<String> target;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "connectorRef", "containerImage", "resources", "endpoint",
      "key", "bucket", "target"})

  public RestoreCacheS3StepInfo(String identifier, String name, Integer retry, ParameterField<String> connectorRef,
      ParameterField<String> containerImage, ContainerResource resources, ParameterField<String> endpoint,
      ParameterField<String> key, ParameterField<String> bucket, ParameterField<String> target) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);

    this.connectorRef = connectorRef;
    this.containerImage =
        Optional.ofNullable(containerImage).orElse(ParameterField.createValueField("plugins/s3-cache:latest"));

    if (containerImage != null && containerImage.fetchFinalValue() == null) {
      this.containerImage = ParameterField.createValueField("plugins/s3-cache:latest");
    }

    this.resources = resources;
    this.endpoint = endpoint;
    this.key = key;
    this.bucket = bucket;
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
