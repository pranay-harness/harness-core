package io.harness.cdng.manifest.mappers;

import static io.harness.cdng.manifest.ManifestType.HelmChart;
import static io.harness.cdng.manifest.ManifestType.K8Manifest;
import static io.harness.cdng.manifest.ManifestType.VALUES;

import static java.lang.String.format;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.kinds.HelmChartManifest;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pms.yaml.ParameterField;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class ManifestOutcomeMapper {
  public List<ManifestOutcome> toManifestOutcome(List<ManifestAttributes> manifestAttributesList) {
    return manifestAttributesList.stream()
        .map(ManifestOutcomeMapper::toManifestOutcome)
        .collect(Collectors.toCollection(LinkedList::new));
  }

  public ManifestOutcome toManifestOutcome(ManifestAttributes manifestAttributes) {
    switch (manifestAttributes.getKind()) {
      case K8Manifest:
        return getK8sOutcome(manifestAttributes);
      case VALUES:
        return getValuesOutcome(manifestAttributes);
      case HelmChart:
        return getHelmChartOutcome(manifestAttributes);
      default:
        throw new UnsupportedOperationException(
            format("Unknown Artifact Config type: [%s]", manifestAttributes.getKind()));
    }
  }

  private K8sManifestOutcome getK8sOutcome(ManifestAttributes manifestAttributes) {
    K8sManifest k8sManifest = (K8sManifest) manifestAttributes;
    boolean skipResourceVersioning = !ParameterField.isNull(k8sManifest.getSkipResourceVersioning())
        && k8sManifest.getSkipResourceVersioning().getValue();

    return K8sManifestOutcome.builder()
        .identifier(k8sManifest.getIdentifier())
        .store(k8sManifest.getStoreConfig())
        .skipResourceVersioning(skipResourceVersioning)
        .build();
  }

  private ValuesManifestOutcome getValuesOutcome(ManifestAttributes manifestAttributes) {
    ValuesManifest attributes = (ValuesManifest) manifestAttributes;
    return ValuesManifestOutcome.builder()
        .identifier(attributes.getIdentifier())
        .store(attributes.getStoreConfig())
        .build();
  }

  private HelmChartManifestOutcome getHelmChartOutcome(ManifestAttributes manifestAttributes) {
    HelmChartManifest helmChartManifest = (HelmChartManifest) manifestAttributes;
    boolean skipResourceVersioning = !ParameterField.isNull(helmChartManifest.getSkipResourceVersioning())
        && helmChartManifest.getSkipResourceVersioning().getValue();
    String manifestStoreKind = helmChartManifest.getStoreConfig().getKind();
    String chartName = null;
    String chartVersion = null;

    if (!ManifestStoreType.isInGitSubset(manifestStoreKind)) {
      if (ParameterField.isNull(helmChartManifest.getChartName())) {
        throw new InvalidArgumentsException(
            Pair.of("chartName", format("required for %s store type", manifestStoreKind)));
      }

      chartName = helmChartManifest.getChartName().getValue();
    } else {
      if (!ParameterField.isNull(helmChartManifest.getChartName())) {
        throw new InvalidArgumentsException(
            Pair.of("chartName", format("not allowed for %s store type", manifestStoreKind)));
      }
    }

    if (!ParameterField.isNull(helmChartManifest.getChartVersion())) {
      if (ManifestStoreType.isInGitSubset(manifestStoreKind)) {
        throw new InvalidArgumentsException(
            Pair.of("chartVersion", format("not allowed for %s store", manifestStoreKind)));
      }

      chartVersion = helmChartManifest.getChartVersion().getValue();
    }

    return HelmChartManifestOutcome.builder()
        .identifier(helmChartManifest.getIdentifier())
        .store(helmChartManifest.getStoreConfig())
        .chartName(chartName)
        .chartVersion(chartVersion)
        .helmVersion(helmChartManifest.getHelmVersion())
        .skipResourceVersioning(skipResourceVersioning)
        .commandFlags(helmChartManifest.getCommandFlags())
        .build();
  }
}
