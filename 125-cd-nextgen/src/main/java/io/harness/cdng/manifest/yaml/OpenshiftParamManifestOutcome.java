package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("openshiftParamManifestOutcome")
@JsonTypeName(ManifestType.OpenshiftParam)
public class OpenshiftParamManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.OpenshiftParam;
  StoreConfig store;
}
