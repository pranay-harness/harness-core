package io.harness.pms.sdk.core.plan.creation.beans;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.yaml.YamlField;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

@Data
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanCreationContext {
  YamlField currentField;
  @Singular("globalContext") Map<String, PlanCreationContextValue> globalContext;

  public static PlanCreationContext cloneWithCurrentField(PlanCreationContext planCreationContext, YamlField field) {
    return PlanCreationContext.builder()
        .currentField(field)
        .globalContext(planCreationContext.getGlobalContext())
        .build();
  }

  public void mergeContextFromPlanCreationResponse(PlanCreationResponse planCreationResponse) {
    if (hasNone(getGlobalContext())) {
      this.setGlobalContext(new HashMap<>());
    }
    this.getGlobalContext().putAll(planCreationResponse.getContextMap());
  }
}
