package io.harness.steps.shellScript.manifest.yaml;

import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.steps.shellScript.manifest.ManifestType;

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
