package io.harness.cdng.sam;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(ManifestType.AwsSamManifest)
public class SamManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.K8Manifest;
  StoreConfig store;
  boolean skipResourceVersioning;
}
