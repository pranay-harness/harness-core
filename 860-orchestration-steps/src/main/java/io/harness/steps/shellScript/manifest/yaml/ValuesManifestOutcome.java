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
@TypeAlias("k8sManifestOutcome")
@JsonTypeName(ManifestType.VALUES)
public class ValuesManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.VALUES;
  StoreConfig store;
}
