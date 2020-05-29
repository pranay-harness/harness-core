package software.wings.service.impl.ci;

import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

/**
 *  Delegate tasks helper for sending tasks on behalf of CI
 */

public interface CIDelegateTaskHelperService {
  K8sTaskExecutionResponse setBuildEnv(
      String k8ConnectorName, String gitConnectorName, String branchName, CIK8PodParams<CIK8ContainerParams> podParams);
  K8sTaskExecutionResponse executeBuildCommand(String k8ConnectorName, K8ExecCommandParams params);
  K8sTaskExecutionResponse cleanupEnv(String k8ConnectorName, String namespace, String podName);
}
