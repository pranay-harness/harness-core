package io.harness.cdng.manifest.yaml.kinds;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.common.SwaggerConstants.BOOLEAN_CLASSPATH;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.manifest.OpenshiftManifestVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
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
@JsonTypeName(ManifestType.OpenshiftTemplate)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = OpenshiftManifestVisitorHelper.class)
@TypeAlias("openshiftManifest")
@OwnedBy(CDC)
public class OpenshiftManifest implements ManifestAttributes, Visitable {
  @EntityIdentifier String identifier;
  @Wither @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;
  @Wither @ApiModelProperty(dataType = BOOLEAN_CLASSPATH) ParameterField<Boolean> skipResourceVersioning;

  @Override
  public StoreConfig getStoreConfig() {
    return this.storeConfigWrapper.getStoreConfig();
  }

  @Override
  public String getKind() {
    return ManifestType.OpenshiftTemplate;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.OPENSHIFT_MANIFEST).isPartOfFQN(false).build();
  }

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    OpenshiftManifest openshiftManifest = (OpenshiftManifest) overrideConfig;
    OpenshiftManifest resultantManifest = this;
    if (openshiftManifest.getStoreConfigWrapper() != null) {
      StoreConfigWrapper storeConfigOverride = openshiftManifest.getStoreConfigWrapper();
      resultantManifest =
          resultantManifest.withStoreConfigWrapper(storeConfigWrapper.applyOverrides(storeConfigOverride));
    }
    if (openshiftManifest.getSkipResourceVersioning() != null) {
      resultantManifest = resultantManifest.withSkipResourceVersioning(openshiftManifest.getSkipResourceVersioning());
    }

    return resultantManifest;
  }
}
