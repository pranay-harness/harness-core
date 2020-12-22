package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.yaml.core.intfc.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = K8sManifestOutcome.class, name = ManifestType.K8Manifest)
  , @JsonSubTypes.Type(value = ValuesManifestOutcome.class, name = ManifestType.VALUES)
})
public interface ManifestOutcome extends Outcome, WithIdentifier {
  String getType();
  StoreConfigWrapper getStore();
}
