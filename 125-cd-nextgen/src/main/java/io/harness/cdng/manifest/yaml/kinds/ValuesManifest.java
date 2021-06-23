package io.harness.cdng.manifest.yaml.kinds;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.visitor.helpers.manifest.ValuesManifestVisitorHelper;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.VALUES)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = ValuesManifestVisitorHelper.class)
@TypeAlias("valuesManifest")
public class ValuesManifest implements ManifestAttributes, Visitable {
  String identifier;
  @Wither @JsonProperty("store") StoreConfigWrapper store;

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    ValuesManifest valuesManifest = (ValuesManifest) overrideConfig;
    ValuesManifest resultantManifest = this;
    if (valuesManifest.getStore() != null) {
      resultantManifest = resultantManifest.withStore(store.applyOverrides(valuesManifest.getStore()));
    }
    return resultantManifest;
  }

  @Override
  public String getKind() {
    return ManifestType.VALUES;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return store.getSpec();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.STORE, store);
    return children;
  }
}
