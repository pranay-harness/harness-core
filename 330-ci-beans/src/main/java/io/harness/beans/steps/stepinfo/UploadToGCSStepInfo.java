package io.harness.beans.steps.stepinfo;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;

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
@JsonTypeName("uploadToGCS")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("uploadToGCSStepInfo")
public class UploadToGCSStepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;
  public static final int DEFAULT_TIMEOUT = 60 * 60 * 2; // 2 hour

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.UPLOAD_GCS).build();
  @JsonIgnore
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(CIStepInfoType.UPLOAD_GCS.name()).build();

  @JsonIgnore private String callbackId;
  @JsonIgnore private Integer port;
  @NotNull @EntityIdentifier private String identifier;
  private String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) private int timeout;

  @NotNull private String connectorRef;
  @JsonIgnore @NotNull private String image;
  private ContainerResource resources;

  // plugin settings
  @NotNull private String bucket;
  @NotNull private String sourcePath;
  @NotNull private String target;

  @Builder
  @ConstructorProperties({"callbackId", "port", "identifier", "name", "retry", "timeout", "connectorRef", "image",
      "resources", "bucket", "sourcePath", "target"})
  public UploadToGCSStepInfo(String callbackId, Integer port, String identifier, String name, Integer retry,
      Integer timeout, String connectorRef, String image, ContainerResource resources, String bucket, String sourcePath,
      String target) {
    this.callbackId = callbackId;
    this.port = port;
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.connectorRef = connectorRef;
    this.image = Optional.ofNullable(image).orElse("plugins/gcs");
    this.resources = resources;

    this.bucket = bucket;
    this.sourcePath = sourcePath;
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
