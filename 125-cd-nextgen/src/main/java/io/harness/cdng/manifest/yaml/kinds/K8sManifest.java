package io.harness.cdng.manifest.yaml.kinds;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.common.SwaggerConstants.BOOLEAN_CLASSPATH;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.manifest.K8sManifestVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@OwnedBy(CDC)
public class K8sManifest implements ManifestAttributes, Visitable {
  @EntityIdentifier String identifier;
  @Wither @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;
  @Wither @ApiModelProperty(dataType = BOOLEAN_CLASSPATH) ParameterField<Boolean> skipResourceVersioning;
  // For Visitor Framework Impl
  String metadata;

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    K8sManifest k8sManifest = (K8sManifest) overrideConfig;
    K8sManifest resultantManifest = this;
    if (k8sManifest.getStoreConfigWrapper() != null) {
      resultantManifest = resultantManifest.withStoreConfigWrapper(
          storeConfigWrapper.applyOverrides(k8sManifest.getStoreConfigWrapper()));
    }
    if (k8sManifest.getSkipResourceVersioning() != null) {
      resultantManifest = resultantManifest.withSkipResourceVersioning(k8sManifest.getSkipResourceVersioning());
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
    children.add(YAMLFieldNameConstants.STORE, storeConfigWrapper);
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_MANIFEST).isPartOfFQN(false).build();
  }
}
