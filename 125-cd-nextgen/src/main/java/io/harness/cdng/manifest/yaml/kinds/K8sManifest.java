package io.harness.cdng.manifest.yaml.kinds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.ValuesPathProvider;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.K8sManifestVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.K8Manifest)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = K8sManifestVisitorHelper.class)
public class K8sManifest implements ManifestAttributes, ValuesPathProvider, Visitable {
  String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore @Wither @Singular List<String> valuesFilePaths;
  @Wither @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;

  @Override
  public List<String> getValuesPathsToFetch() {
    return valuesFilePaths;
  }

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    K8sManifest k8sManifest = (K8sManifest) overrideConfig;
    K8sManifest resultantManifest = this;
    if (EmptyPredicate.isNotEmpty(k8sManifest.getValuesFilePaths())) {
      resultantManifest = resultantManifest.withValuesFilePaths(k8sManifest.getValuesFilePaths());
    }
    if (k8sManifest.getStoreConfigWrapper() != null) {
      resultantManifest = resultantManifest.withStoreConfigWrapper(
          storeConfigWrapper.applyOverrides(k8sManifest.getStoreConfigWrapper()));
    }
    return resultantManifest;
  }

  @Override
  public String getKind() {
    return ManifestType.K8Manifest;
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
}
