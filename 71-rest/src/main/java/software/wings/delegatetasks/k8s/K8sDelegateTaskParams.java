package software.wings.delegatetasks.k8s;

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
}
