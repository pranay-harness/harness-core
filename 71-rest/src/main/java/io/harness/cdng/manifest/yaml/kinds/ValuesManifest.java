package io.harness.cdng.manifest.yaml.kinds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.VALUES)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValuesManifest implements ManifestAttributes {
  String identifier;
  @Wither @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    ValuesManifest valuesManifest = (ValuesManifest) overrideConfig;
    ValuesManifest resultantManifest = this;
    if (valuesManifest.getStoreConfigWrapper() != null) {
      resultantManifest = resultantManifest.withStoreConfigWrapper(
          storeConfigWrapper.applyOverrides(valuesManifest.getStoreConfigWrapper()));
    }
    return resultantManifest;
  }

  @Override
  public String getKind() {
    return ManifestType.VALUES;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return storeConfigWrapper.getStoreConfig();
  }
}
