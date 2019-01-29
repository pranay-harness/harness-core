package software.wings.api.k8s;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sElement {
  String releaseName;
  Integer releaseNumber;
  Integer targetInstances;
  String primaryServiceName;
  String stageServiceName;
  String currentReleaseWorkload;
  String previousReleaseWorkload;
  String canaryWorkload;
  boolean isCanary;
}
