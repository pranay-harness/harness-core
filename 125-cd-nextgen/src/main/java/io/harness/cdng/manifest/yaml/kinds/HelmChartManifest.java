package io.harness.cdng.manifest.yaml.kinds;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.common.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.beans.common.SwaggerConstants.STRING_CLASSPATH;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.HelmManifestCommandFlag;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.manifest.HelmChartManifestVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.k8s.model.HelmVersion;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.HelmChart)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = HelmChartManifestVisitorHelper.class)
@TypeAlias("helmChartManifest")
public class HelmChartManifest implements ManifestAttributes, Visitable {
  @EntityIdentifier String identifier;
  @Wither @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;
  @Wither @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> chartName;
  @Wither @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> chartVersion;
  @Wither HelmVersion helmVersion;
  @Wither @ApiModelProperty(dataType = BOOLEAN_CLASSPATH) ParameterField<Boolean> skipResourceVersioning;
  @Wither List<HelmManifestCommandFlag> commandFlags;

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    HelmChartManifest helmChartManifest = (HelmChartManifest) overrideConfig;
    HelmChartManifest resultantManifest = this;
    if (helmChartManifest.getStoreConfigWrapper() != null) {
      StoreConfigWrapper storeConfigOverride = helmChartManifest.getStoreConfigWrapper();
      resultantManifest =
          resultantManifest.withStoreConfigWrapper(storeConfigWrapper.applyOverrides(storeConfigOverride));
    }

    if (!ParameterField.isNull(helmChartManifest.getChartName())) {
      resultantManifest = resultantManifest.withChartName(helmChartManifest.getChartName());
    }

    if (!ParameterField.isNull(helmChartManifest.getChartVersion())) {
      resultantManifest = resultantManifest.withChartVersion(helmChartManifest.getChartVersion());
    }

    if (helmChartManifest.getHelmVersion() != null) {
      resultantManifest = resultantManifest.withHelmVersion(helmChartManifest.getHelmVersion());
    }
    if (helmChartManifest.getSkipResourceVersioning() != null) {
      resultantManifest = resultantManifest.withSkipResourceVersioning(helmChartManifest.getSkipResourceVersioning());
    }

    if (helmChartManifest.getCommandFlags() != null) {
      resultantManifest = resultantManifest.withCommandFlags(new ArrayList<>(helmChartManifest.getCommandFlags()));
    }

    return resultantManifest;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.STORE, storeConfigWrapper);
    return children;
  }

  @Override
  public String getKind() {
    return ManifestType.HelmChart;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return this.storeConfigWrapper.getStoreConfig();
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.HELM_CHART_MANIFEST).isPartOfFQN(false).build();
  }
}
