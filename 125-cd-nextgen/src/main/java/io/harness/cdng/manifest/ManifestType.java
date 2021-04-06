package io.harness.cdng.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public interface ManifestType {
  String K8Manifest = "K8sManifest";
  String VALUES = "Values";
  String CONFIG_FILE = "configFiles";
  String HelmChart = "HelmChart";
  String Kustomize = "Kustomize";
  String OpenshiftTemplate = "OpenshiftTemplate";
  String OpenshiftParam = "OpenshiftParam";
}
