package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.k8s.model.HelmVersion;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(CDP)
@JsonTypeName(ManifestType.HelmChart)
@TypeAlias("helmChartManifestOutcome")
@FieldNameConstants(innerTypeName = "HelmChartManifestOutcomeKeys")
@RecasterAlias("io.harness.cdng.manifest.yaml.HelmChartManifestOutcome")
public class HelmChartManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.HelmChart;
  StoreConfig store;
  ParameterField<String> chartName;
  ParameterField<String> chartVersion;
  ParameterField<Boolean> skipResourceVersioning;
  HelmVersion helmVersion;
  List<HelmManifestCommandFlag> commandFlags;
}
