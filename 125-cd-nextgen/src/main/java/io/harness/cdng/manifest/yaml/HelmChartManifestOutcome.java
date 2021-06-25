package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.k8s.model.HelmVersion;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(ManifestType.HelmChart)
@TypeAlias("helmChartManifestOutcome")
public class HelmChartManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.HelmChart;
  StoreConfig store;
  String chartName;
  String chartVersion;
  HelmVersion helmVersion;
  boolean skipResourceVersioning;
  List<HelmManifestCommandFlag> commandFlags;
}
