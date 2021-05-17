package io.harness.cdng.manifest.yaml;

import io.harness.beans.WithIdentifier;
import io.harness.cdng.manifest.ManifestType;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = K8sManifestOutcome.class, name = ManifestType.K8Manifest)
  , @JsonSubTypes.Type(value = ValuesManifestOutcome.class, name = ManifestType.VALUES),
      @JsonSubTypes.Type(value = HelmChartManifestOutcome.class, name = ManifestType.HelmChart),
      @JsonSubTypes.Type(value = KustomizeManifestOutcome.class, name = ManifestType.Kustomize),
      @JsonSubTypes.Type(value = OpenshiftManifestOutcome.class, name = ManifestType.OpenshiftTemplate),
      @JsonSubTypes.Type(value = OpenshiftParamManifestOutcome.class, name = ManifestType.OpenshiftParam)
})
public interface ManifestOutcome extends Outcome, WithIdentifier {
  String getType();
}
