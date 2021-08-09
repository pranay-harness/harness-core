package io.harness.polling.bean.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.polling.bean.PollingInfo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@OwnedBy(HarnessTeam.CDC)
@JsonTypeName("MANIFEST")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public interface ManifestInfo extends PollingInfo {
  String getType();
  StoreConfig getStore();
  ManifestOutcome toManifestOutcome();
}
