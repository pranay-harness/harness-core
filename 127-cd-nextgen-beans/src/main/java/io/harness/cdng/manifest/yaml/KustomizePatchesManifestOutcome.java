package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("kustomizeManifestOutcome")
@JsonTypeName(ManifestType.KustomizePatches)
@OwnedBy(CDP)
public class KustomizePatchesManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.KustomizePatches;
  StoreConfig store;
  int order;
}
