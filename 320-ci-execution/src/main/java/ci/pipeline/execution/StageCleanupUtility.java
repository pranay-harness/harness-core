package ci.pipeline.execution;

import static io.harness.beans.sweepingoutputs.StageInfraDetails.STAGE_INFRA_DETAILS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.sweepingoutputs.K8StageInfraDetails;
import io.harness.beans.sweepingoutputs.StageDetails;
import io.harness.beans.sweepingoutputs.StageInfraDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.delegate.beans.ci.CICleanupTaskParams;
import io.harness.delegate.beans.ci.awsvm.CIAwsVmCleanupTaskParams;
import io.harness.delegate.beans.ci.k8s.CIK8CleanupTaskParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.environment.EnvironmentOutcome;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CI)
public class StageCleanupUtility {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ConnectorUtils connectorUtils;

  public CICleanupTaskParams buildAndfetchCleanUpParameters(Ambiance ambiance) {
    StageInfraDetails stageInfraDetails = (StageInfraDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(STAGE_INFRA_DETAILS));
    if (stageInfraDetails.getType() == StageInfraDetails.Type.K8) {
      K8StageInfraDetails k8StageInfraDetails = (K8StageInfraDetails) stageInfraDetails;
      return buildK8CleanupParameters(k8StageInfraDetails, ambiance);
    } else if (stageInfraDetails.getType() == StageInfraDetails.Type.AWS_VM) {
      return buildAwsVmCleanupParameters(ambiance);
    } else {
      throw new CIStageExecutionException("Unknown infra type");
    }
  }

  public CIK8CleanupTaskParams buildK8CleanupParameters(K8StageInfraDetails k8StageInfraDetails, Ambiance ambiance) {
    Infrastructure infrastructure = k8StageInfraDetails.getInfrastructure();
    if (infrastructure == null || ((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    // It should always resolved to K8sDirectInfraYaml
    K8sDirectInfraYaml k8sDirectInfraYaml = (K8sDirectInfraYaml) infrastructure;

    final String clusterConnectorRef = k8sDirectInfraYaml.getSpec().getConnectorRef();
    final String namespace = k8sDirectInfraYaml.getSpec().getNamespace();
    final List<String> podNames = new ArrayList<>();
    podNames.add(k8StageInfraDetails.getPodName());

    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);

    ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(ngAccess, clusterConnectorRef);

    return CIK8CleanupTaskParams.builder()
        .k8sConnector(connectorDetails)
        .cleanupContainerNames(k8StageInfraDetails.getContainerNames())
        .namespace(namespace)
        .podNameList(podNames)
        .serviceNameList(new ArrayList<>())
        .build();
  }

  public CIAwsVmCleanupTaskParams buildAwsVmCleanupParameters(Ambiance ambiance) {
    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputResolver.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.stageDetails));
    if (!optionalSweepingOutput.isFound()) {
      throw new CIStageExecutionException("Stage details sweeping output cannot be empty");
    }

    StageDetails stageDetails = (StageDetails) optionalSweepingOutput.getOutput();
    return CIAwsVmCleanupTaskParams.builder().stageRuntimeId(stageDetails.getStageRuntimeID()).build();
  }
}
