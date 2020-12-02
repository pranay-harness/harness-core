package io.harness.pms.sdk.core.plan.creation.beans;

import io.harness.pms.yaml.YamlField;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanCreationContext {
  YamlField currentField;

  public static PlanCreationContext cloneWithCurrentField(PlanCreationContext planCreationContext, YamlField field) {
    return PlanCreationContext.builder().currentField(field).build();
  }
}
