package software.wings.delegatetasks.k8s;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sCommandTaskParams {
  String kubectlPath;
  String kubeconfigPath;
  String workingDirectory;
}
