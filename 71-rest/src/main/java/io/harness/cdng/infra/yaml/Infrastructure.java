package io.harness.cdng.infra.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.data.Outcome;
import io.harness.facilitator.PassThroughData;
import io.harness.yaml.core.intfc.OverridesApplier;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface Infrastructure extends Outcome, PassThroughData, OverridesApplier<Infrastructure> {
  @JsonIgnore InfraMapping getInfraMapping();
  @JsonIgnore String getKind();
}
