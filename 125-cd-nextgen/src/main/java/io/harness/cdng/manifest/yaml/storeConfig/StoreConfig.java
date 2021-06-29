package io.harness.cdng.manifest.yaml.storeConfig;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

@OwnedBy(PIPELINE)
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface StoreConfig extends OverridesApplier<StoreConfig>, Serializable {
  @JsonIgnore String getKind();
  @JsonIgnore StoreConfig cloneInternal();
  @JsonIgnore ParameterField<String> getConnectorReference();
}
