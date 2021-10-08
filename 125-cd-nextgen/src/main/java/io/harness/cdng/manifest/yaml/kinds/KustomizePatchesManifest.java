package io.harness.cdng.manifest.yaml.kinds;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.visitor.helpers.manifest.KustomizePatchesManifestVisitorHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;
import io.harness.pms.yaml.YAMLFieldNameConstants;
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
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.KustomizePatches)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = KustomizePatchesManifestVisitorHelper.class)
@TypeAlias("kustomizePatchesManifest")
@RecasterAlias("io.harness.cdng.manifest.yaml.kinds.KustomizePatchesManifest")
public class KustomizePatchesManifest implements ManifestAttributes, Visitable {
  String identifier;
  @Wither
  @JsonProperty("store")
  @ApiModelProperty(dataType = "io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper")
  @SkipAutoEvaluation
  ParameterField<StoreConfigWrapper> store;

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    KustomizePatchesManifest kustomizePatchesManifest = (KustomizePatchesManifest) overrideConfig;
    KustomizePatchesManifest resultantManifest = this;
    if (kustomizePatchesManifest.getStore() != null && kustomizePatchesManifest.getStore().getValue() != null) {
      resultantManifest = resultantManifest.withStore(ParameterField.createValueField(
          store.getValue().applyOverrides(kustomizePatchesManifest.getStore().getValue())));
    }
    return resultantManifest;
  }

  @Override
  public String getKind() {
    return ManifestType.KustomizePatches;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return store.getValue().getSpec();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.STORE, store.getValue());
    return children;
  }

  @Override
  public ManifestAttributeStepParameters getManifestAttributeStepParameters() {
    return new KustomizePatchesManifestStepParameters(
        identifier, StoreConfigWrapper.StoreConfigWrapperParameters.fromStoreConfigWrapper(store.getValue()));
  }

  @Value
  public static class KustomizePatchesManifestStepParameters implements ManifestAttributeStepParameters {
    String identifier;
    StoreConfigWrapper.StoreConfigWrapperParameters store;
  }
}
