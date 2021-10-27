package io.harness.cvng.servicelevelobjective.beans;

import static io.harness.cvng.CVConstants.SLI_METRIC_TYPE;

import io.harness.cvng.servicelevelobjective.beans.slimetricspec.SLIMetricSpec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceLevelIndicatorSpec {
  @NotNull @JsonProperty(SLI_METRIC_TYPE) SLIMetricType type;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = SLI_METRIC_TYPE, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true)
  @Valid
  @NotNull
  SLIMetricSpec spec;
}
