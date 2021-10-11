package io.harness.k8s.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sDelegateTaskParams {
  String kubectlPath;
  String kubeconfigPath;
  String workingDirectory;
  String goTemplateClientPath;
  String helmPath;
  String ocPath;
  String kustomizeBinaryPath;
  String kubectlVersion;
  String helmVersion;
}
