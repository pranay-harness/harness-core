package ci.pipeline.execution;

import static io.harness.beans.sweepingoutputs.PodCleanupDetails.CLEANUP_DETAILS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.sweepingoutputs.PodCleanupDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.delegate.beans.ci.CIK8CleanupTaskParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.stateutils.buildstate.ConnectorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CI)
public class PodCleanupUtility {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ConnectorUtils connectorUtils;
  public CIK8CleanupTaskParams buildAndfetchCleanUpParameters(Ambiance ambiance) {
    PodCleanupDetails podCleanupDetails = (PodCleanupDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(CLEANUP_DETAILS));

    Infrastructure infrastructure = podCleanupDetails.getInfrastructure();

    if (infrastructure == null || ((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    // It should always resolved to K8sDirectInfraYaml
    K8sDirectInfraYaml k8sDirectInfraYaml = (K8sDirectInfraYaml) infrastructure;

    final String clusterConnectorRef = k8sDirectInfraYaml.getSpec().getConnectorRef();
    final String namespace = k8sDirectInfraYaml.getSpec().getNamespace();
    final List<String> podNames = new ArrayList<>();
    podNames.add(podCleanupDetails.getPodName());

    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);

    ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(ngAccess, clusterConnectorRef);

    return CIK8CleanupTaskParams.builder()
        .k8sConnector(connectorDetails)
        .cleanupContainerNames(podCleanupDetails.getCleanUpContainerNames())
        .namespace(namespace)
        .podNameList(podNames)
        .serviceNameList(new ArrayList<>())
        .build();
  }
}
