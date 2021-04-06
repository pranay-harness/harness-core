package io.harness.cdng.manifest;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface ManifestType {
  String K8Manifest = "K8sManifest";
  String VALUES = "Values";
  String CONFIG_FILE = "configFiles";
  String HelmChart = "HelmChart";
  String Kustomize = "Kustomize";
  String OpenshiftTemplate = "OpenshiftTemplate";
  String OpenshiftParam = "OpenshiftParam";
  String TerraformConfig = "TerraformConfig";
  String TerraformVarFile = "TerraformVarFile";
}
