package io.harness.steps.shellScript.manifest.yaml;

import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.steps.shellScript.manifest.ManifestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.steps.shellScript.manifest.yaml.ManifestOutcome;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(ManifestType.OpenshiftTemplate)
@TypeAlias("openshiftManifestOutcome")
public class OpenshiftManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.OpenshiftTemplate;
  StoreConfig store;
  boolean skipResourceVersioning;
}
