package io.harness.cdng.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.manifest.ManifestType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

@OwnedBy(HarnessTeam.CDP)
public enum ManifestConfigType {
  @JsonProperty(io.harness.manifest.ManifestType.HelmChart) HELM_CHART(io.harness.manifest.ManifestType.HelmChart),
  @JsonProperty(io.harness.manifest.ManifestType.K8Manifest) K8_MANIFEST(io.harness.manifest.ManifestType.K8Manifest),
  @JsonProperty(io.harness.manifest.ManifestType.Kustomize) KUSTOMIZE(io.harness.manifest.ManifestType.Kustomize),
  @JsonProperty(io.harness.manifest.ManifestType.OpenshiftParam)
  OPEN_SHIFT_PARAM(io.harness.manifest.ManifestType.OpenshiftParam),
  @JsonProperty(io.harness.manifest.ManifestType.OpenshiftTemplate)
  OPEN_SHIFT_TEMPLATE(io.harness.manifest.ManifestType.OpenshiftTemplate),
  @JsonProperty(io.harness.manifest.ManifestType.VALUES) VALUES(ManifestType.VALUES);
  private final String displayName;

  ManifestConfigType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  @JsonCreator
  public static ManifestConfigType getManifestConfigType(@JsonProperty("type") String displayName) {
    for (ManifestConfigType manifestConfigType : ManifestConfigType.values()) {
      if (manifestConfigType.displayName.equalsIgnoreCase(displayName)) {
        return manifestConfigType;
      }
    }

    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", displayName, Arrays.toString(ManifestConfigType.values())));
  }
}
