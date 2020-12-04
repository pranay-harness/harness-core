package io.harness.beans.steps.stepinfo;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.steps.StepType;

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
@JsonTypeName("restoreCacheS3")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("restoreCacheS3StepInfo")
public class RestoreCacheS3StepInfo implements PluginCompatibleStep {
  public static final int DEFAULT_RETRY = 1;
  public static final int DEFAULT_TIMEOUT = 60 * 60 * 2; // 2 hour

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo =
      TypeInfo.builder()
          .stepInfoType(CIStepInfoType.RESTORE_CACHE_S3)
          .stepType(StepType.newBuilder().setType(CIStepInfoType.RESTORE_CACHE_S3.name()).build())
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
  private String endpoint;
  @NotNull private String key;
  @NotNull private String bucket;
  private String target;

  @Builder
  @ConstructorProperties({"callbackId", "port", "identifier", "name", "retry", "timeout", "connectorRef", "image",
      "resources", "endpoint", "key", "bucket", "target"})
  public RestoreCacheS3StepInfo(String callbackId, Integer port, String identifier, String name, Integer retry,
      Integer timeout, String connectorRef, String image, ContainerResource resources, String endpoint, String key,
      String bucket, String target) {
    this.callbackId = callbackId;
    this.port = port;
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.connectorRef = connectorRef;
    this.image = Optional.ofNullable(image).orElse("plugins/s3-cache:latest");
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
    return typeInfo.getStepType();
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }
}
