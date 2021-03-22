package io.harness.steps.shellScript.manifest.yaml;

import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.steps.shellScript.manifest.ManifestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(ManifestType.Kustomize)
@TypeAlias("kustomizeManifestOutcome")
public class KustomizeManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.Kustomize;
  StoreConfig store;
  String pluginPath;
  boolean skipResourceVersioning;
}
