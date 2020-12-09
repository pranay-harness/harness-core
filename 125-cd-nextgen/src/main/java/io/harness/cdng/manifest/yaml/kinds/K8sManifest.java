package io.harness.cdng.manifest.yaml.kinds;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.ValuesPathProvider;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.manifest.K8sManifestVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.K8Manifest)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = K8sManifestVisitorHelper.class)
@TypeAlias("k8sManifest")
public class K8sManifest implements ManifestAttributes, ValuesPathProvider, Visitable {
  @EntityIdentifier String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore @Wither ParameterField<List<String>> valuesFilePaths;
  @Wither @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public List<String> getValuesPathsToFetch() {
    return valuesFilePaths.getValue();
  }

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    K8sManifest k8sManifest = (K8sManifest) overrideConfig;
    K8sManifest resultantManifest = this;
    if (k8sManifest.getValuesFilePaths() != null) {
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

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_MANIFEST).build();
  }
}
