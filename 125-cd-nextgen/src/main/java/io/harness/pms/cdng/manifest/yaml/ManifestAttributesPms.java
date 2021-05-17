package io.harness.pms.cdng.manifest.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.beans.WithIdentifier;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY)
public interface ManifestAttributesPms extends WithIdentifier, OverridesApplier<ManifestAttributesPms>, Serializable {
  @JsonIgnore String getKind();
  void setIdentifier(String identifier);
  @JsonIgnore
  default StoreConfigPms getStoreConfigPms() {
    return null;
  }
  @Override @JsonIgnore String getIdentifier();
}
