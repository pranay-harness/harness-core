package io.harness.manifest;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.manifest.storeConfig.StoreConfig;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(CDP)
@JsonTypeName(ManifestType.OpenshiftTemplate)
@TypeAlias("openshiftManifestOutcome")
@FieldNameConstants(innerTypeName = "OpenshiftManifestOutcomeKeys")
public class OpenshiftManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.OpenshiftTemplate;
  StoreConfig store;
  ParameterField<Boolean> skipResourceVersioning;
}
