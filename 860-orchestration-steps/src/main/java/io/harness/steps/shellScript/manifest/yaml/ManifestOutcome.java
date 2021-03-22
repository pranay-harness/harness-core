package io.harness.steps.shellScript.manifest.yaml;

import io.harness.steps.shellScript.manifest.ManifestType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.yaml.core.intfc.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = K8sManifestOutcome.class, name = ManifestType.K8Manifest)
  , @JsonSubTypes.Type(value = ValuesManifestOutcome.class, name = ManifestType.VALUES),
      @JsonSubTypes.Type(value = HelmChartManifestOutcome.class, name = ManifestType.HelmChart)
})
public interface ManifestOutcome extends Outcome, WithIdentifier {
  String getType();
}
