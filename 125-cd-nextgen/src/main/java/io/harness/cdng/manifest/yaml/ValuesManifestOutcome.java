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
@JsonTypeName(ManifestType.VALUES)
public class ValuesManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.VALUES;
  StoreConfig store;
}
