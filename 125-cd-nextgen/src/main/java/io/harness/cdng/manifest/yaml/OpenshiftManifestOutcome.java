package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
