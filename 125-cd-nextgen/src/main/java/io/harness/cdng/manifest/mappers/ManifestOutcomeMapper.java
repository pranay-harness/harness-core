package io.harness.cdng.manifest.mappers;

import static io.harness.cdng.manifest.ManifestType.K8Manifest;
import static io.harness.cdng.manifest.ManifestType.VALUES;

import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

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
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown Artifact Config type: [%s]", manifestAttributes.getKind()));
    }
  }

  private K8sManifestOutcome getK8sOutcome(ManifestAttributes manifestAttributes) {
    K8sManifest k8sManifest = (K8sManifest) manifestAttributes;
    return K8sManifestOutcome.builder()
        .identifier(k8sManifest.getIdentifier())
        .store(k8sManifest.getStoreConfigWrapper())
        .build();
  }

  private ValuesManifestOutcome getValuesOutcome(ManifestAttributes manifestAttributes) {
    ValuesManifest attributes = (ValuesManifest) manifestAttributes;
    return ValuesManifestOutcome.builder()
        .identifier(attributes.getIdentifier())
        .store(attributes.getStoreConfigWrapper())
        .build();
  }
}
