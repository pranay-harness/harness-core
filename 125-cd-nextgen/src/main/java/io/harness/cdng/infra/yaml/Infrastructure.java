package io.harness.cdng.infra.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(CDC)
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface Infrastructure extends StepParameters, PassThroughData, OverridesApplier<Infrastructure> {
  @JsonIgnore InfraMapping getInfraMapping();
  @JsonIgnore String getKind();
  @JsonIgnore ParameterField<String> getConnectorReference();
  @JsonIgnore String[] getInfrastructureKeyValues();
}
