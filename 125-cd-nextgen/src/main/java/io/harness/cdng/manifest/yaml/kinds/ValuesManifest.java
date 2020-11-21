package io.harness.cdng.manifest.yaml.kinds;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.manifest.ValuesManifestVisitorHelper;
import io.harness.walktree.beans.LevelNode;
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

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.VALUES)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = ValuesManifestVisitorHelper.class)
public class ValuesManifest implements ManifestAttributes, Visitable {
  String identifier;
  @Wither @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    ValuesManifest valuesManifest = (ValuesManifest) overrideConfig;
    ValuesManifest resultantManifest = this;
    if (valuesManifest.getStoreConfigWrapper() != null) {
      resultantManifest = resultantManifest.withStoreConfigWrapper(
          storeConfigWrapper.applyOverrides(valuesManifest.getStoreConfigWrapper()));
    }
    return resultantManifest;
  }

  @Override
  public String getKind() {
    return ManifestType.VALUES;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return storeConfigWrapper.getStoreConfig();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("storeConfigWrapper", storeConfigWrapper);
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SPEC).build();
  }
}
