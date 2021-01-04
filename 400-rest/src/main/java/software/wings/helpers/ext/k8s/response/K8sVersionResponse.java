package software.wings.helpers.ext.k8s.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sVersionResponse implements K8sTaskResponse {
  String serverMajorVersion;
  String serverMinorVersion;
  String platform;
  String gitVersion;
  String gitCommit;
}
