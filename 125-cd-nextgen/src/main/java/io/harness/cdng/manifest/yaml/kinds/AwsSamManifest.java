package io.harness.cdng.manifest.yaml.kinds;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.data.validator.EntityIdentifier;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsSamManifest implements ManifestAttributes {
  @EntityIdentifier private String identifier;
  private StoreConfigWrapper store;

  @Override
  public String getKind() {
    return ManifestType.AwsSamManifest;
  }

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    return null;
  }
}
