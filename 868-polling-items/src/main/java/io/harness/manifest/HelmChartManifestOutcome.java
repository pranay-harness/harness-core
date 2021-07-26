package io.harness.manifest;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.HelmVersion;
import io.harness.manifest.storeConfig.StoreConfig;
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
