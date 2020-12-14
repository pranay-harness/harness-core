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
@JsonTypeName("restoreCacheGCS")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("restoreCacheGCSStepInfo")
public class RestoreCacheGCSStepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;
  public static final int DEFAULT_TIMEOUT = 60 * 60 * 2; // 2 hour

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo =
      TypeInfo.builder()
          .stepInfoType(CIStepInfoType.RESTORE_CACHE_GCS)
          .stepType(StepType.newBuilder().setType(CIStepInfoType.RESTORE_CACHE_GCS.name()).build())
          .build();

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
  @NotNull private String key;
  @NotNull private String bucket;
  private String target;

  @Builder
  @ConstructorProperties({"callbackId", "port", "identifier", "name", "retry", "timeout", "connectorRef", "image",
      "resources", "key", "bucket", "target"})
  public RestoreCacheGCSStepInfo(String callbackId, Integer port, String identifier, String name, Integer retry,
      Integer timeout, String connectorRef, String image, ContainerResource resources, String key, String bucket,
      String target) {
    this.callbackId = callbackId;
    this.port = port;
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.connectorRef = connectorRef;
    this.image = Optional.ofNullable(image).orElse("homerovalle/drone-gcs-cache:latest");
    this.resources = resources;
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
    return typeInfo.getStepType();
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }
}
