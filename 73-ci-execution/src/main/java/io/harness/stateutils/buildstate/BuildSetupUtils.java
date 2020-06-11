package io.harness.stateutils.buildstate;

import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.exception.InvalidRequestException;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.references.SweepingOutputRefObject;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
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
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  public RestResponse<K8sTaskExecutionResponse> executeCISetupTask(
      BuildEnvSetupStepInfo buildEnvSetupStepInfo, Ambiance ambiance) {
    switch (buildEnvSetupStepInfo.getSetupEnv().getBuildJobEnvInfo().getType()) {
      case K8:
        try {
          K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
              ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

          final String namespace = k8PodDetails.getNamespace();
          final String clusterName = k8PodDetails.getClusterName();
          K8BuildJobEnvInfo k8BuildJobEnvInfo =
              (K8BuildJobEnvInfo) buildEnvSetupStepInfo.getSetupEnv().getBuildJobEnvInfo();
          // Supporting single pod currently
          Optional<PodSetupInfo> podSetupInfoOpt =
              k8BuildJobEnvInfo.getPodsSetupInfo().getPodSetupInfoList().stream().findFirst();
          if (!podSetupInfoOpt.isPresent()) {
            throw new InvalidRequestException("Pod setup info can not be empty");
          }

          PodSetupInfo podSetupInfo = podSetupInfoOpt.get();
          // TODO Use k8 connector from element input
          return SafeHttpCall.execute(managerCIResource.createK8PodTask(clusterName,
              buildEnvSetupStepInfo.getSetupEnv().getGitConnectorIdentifier(),
              buildEnvSetupStepInfo.getSetupEnv().getBranchName(),
              k8BuildSetupUtils.getPodParams(podSetupInfo, namespace)));

        } catch (Exception e) {
          logger.error("build state execution failed", e);
        }
        break;
      default:
        unhandled(buildEnvSetupStepInfo.getSetupEnv().getBuildJobEnvInfo().getType());
    }
    return null;
  }
}
