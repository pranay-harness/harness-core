package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("k8sManifestOutcome")
@JsonTypeName(ManifestType.K8Manifest)
public class K8sManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.K8Manifest;
  StoreConfig store;
  boolean skipResourceVersioning;
}
