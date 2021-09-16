package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(CDP)
@TypeAlias("k8sManifestOutcome")
@JsonTypeName(ManifestType.K8Manifest)
@FieldNameConstants(innerTypeName = "K8sManifestOutcomeKeys")
@RecasterAlias("io.harness.cdng.manifest.yaml.K8sManifestOutcome")
public class K8sManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.K8Manifest;
  StoreConfig store;
  ParameterField<Boolean> skipResourceVersioning;
}
