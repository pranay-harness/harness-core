package io.harness.cdng.manifest.yaml.kinds;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.common.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.bool;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.visitor.helpers.manifest.KustomizeManifestVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.YamlSchemaTypes;

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
@JsonTypeName(ManifestType.Kustomize)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = KustomizeManifestVisitorHelper.class)
@TypeAlias("kustomizeManifest")
@OwnedBy(CDC)
public class KustomizeManifest implements ManifestAttributes, Visitable {
  @EntityIdentifier String identifier;
  @Wither @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;
  @Wither @YamlSchemaTypes({string, bool}) ParameterField<Boolean> skipResourceVersioning;
  @Wither @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> pluginPath;

  @Override
  public String getKind() {
    return ManifestType.Kustomize;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return this.storeConfigWrapper.getStoreConfig();
  }

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    KustomizeManifest kustomizeManifest = (KustomizeManifest) overrideConfig;
    KustomizeManifest resultantManifest = this;
    if (kustomizeManifest.getStoreConfigWrapper() != null) {
      StoreConfigWrapper storeConfigOverride = kustomizeManifest.getStoreConfigWrapper();
      resultantManifest =
          resultantManifest.withStoreConfigWrapper(storeConfigWrapper.applyOverrides(storeConfigOverride));
    }
    if (kustomizeManifest.getSkipResourceVersioning() != null) {
      resultantManifest = resultantManifest.withSkipResourceVersioning(kustomizeManifest.getSkipResourceVersioning());
    }

    if (kustomizeManifest.getPluginPath() != null) {
      resultantManifest = resultantManifest.withPluginPath(kustomizeManifest.getPluginPath());
    }

    return resultantManifest;
  }
}
