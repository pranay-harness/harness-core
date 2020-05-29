package io.harness.stateutils.buildstate;

import static io.harness.govern.Switch.unhandled;
import static software.wings.common.CICommonPodConstants.CLUSTER_EXPRESSION;
import static software.wings.common.CICommonPodConstants.NAMESPACE_EXPRESSION;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.exception.InvalidRequestException;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.Optional;

@Singleton
@Slf4j
public class BuildSetupUtils {
  @Inject private ManagerCIResource managerCIResource;
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;
  @Inject private EngineExpressionService engineExpressionService;

  public RestResponse<K8sTaskExecutionResponse> executeCISetupTask(
      BuildEnvSetupStepInfo buildEnvSetupStepInfo, Ambiance ambiance) {
    switch (buildEnvSetupStepInfo.getBuildJobEnvInfo().getType()) {
      case K8:
        try {
          final String clusterName = engineExpressionService.renderExpression(ambiance, CLUSTER_EXPRESSION);
          final String namespace = engineExpressionService.renderExpression(ambiance, NAMESPACE_EXPRESSION);
          K8BuildJobEnvInfo k8BuildJobEnvInfo = (K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo();
          // Supporting single pod currently
          Optional<PodSetupInfo> podSetupInfoOpt =
              k8BuildJobEnvInfo.getPodsSetupInfo().getPodSetupInfoList().stream().findFirst();
          if (!podSetupInfoOpt.isPresent()) {
            throw new InvalidRequestException("Pod setup info can not be empty");
          }

          PodSetupInfo podSetupInfo = podSetupInfoOpt.get();
          // TODO Use k8 connector from element input
          return SafeHttpCall.execute(
              managerCIResource.createK8PodTask(clusterName, buildEnvSetupStepInfo.getGitConnectorIdentifier(),
                  buildEnvSetupStepInfo.getBranchName(), k8BuildSetupUtils.getPodParams(podSetupInfo, namespace)));

        } catch (Exception e) {
          logger.error("build state execution failed", e);
        }
        break;
      default:
        unhandled(buildEnvSetupStepInfo.getBuildJobEnvInfo().getType());
    }
    return null;
  }
}
