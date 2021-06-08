package io.harness.cdng.manifest.yaml.kinds;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.data.validator.EntityIdentifier;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(ManifestType.AwsSamManifest)
public class AwsSamManifest implements ManifestAttributes, ManifestOutcome {
  @EntityIdentifier private String identifier;
  private StoreConfigWrapper store;

  @Override
  public String getKind() {
    return ManifestType.AwsSamManifest;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return store.getSpec();
  }

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    return null;
  }

  @Override
  public String getType() {
    return ManifestType.AwsSamManifest;
  }
}
