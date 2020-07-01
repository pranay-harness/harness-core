package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.data.validator.EntityIdentifier;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.facilitator.FacilitatorType;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Value;
import software.wings.jersey.JsonViews;

import java.beans.ConstructorProperties;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Value
@JsonTypeName("restoreCache")
public class RestoreCacheStepInfo implements CIStepInfo, GenericStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 1200;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo =
      TypeInfo.builder()
          .stepInfoType(CIStepInfoType.RESTORE_CACHE)
          .stepType(StepType.builder().type(CIStepInfoType.RESTORE_CACHE.name()).build())
          .build();

  @NotNull @EntityIdentifier private String identifier;
  private String displayName;
  @Min(MIN_RETRY) @Max(MAX_RETRY) int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) int timeout;
  @NotNull private RestoreCache restoreCache;

  @Builder
  @ConstructorProperties({"identifier", "displayName", "retry", "timeout", "restoreCache"})
  public RestoreCacheStepInfo(
      String identifier, String displayName, Integer retry, Integer timeout, RestoreCache restoreCache) {
    this.identifier = identifier;
    this.displayName = displayName;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.restoreCache = restoreCache;
  }

  @Value
  @Builder
  public static class RestoreCache {
    @NotNull private String key;
    private boolean failIfNotExist;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }

  @Override
  public StepType getStepType() {
    return typeInfo.getStepType();
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.SYNC;
  }
}
