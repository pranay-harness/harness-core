package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.k8s.K8sRollingOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.entity.DeploymentEvent;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;

import com.google.inject.Inject;
import groovy.util.logging.Slf4j;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class DeploymentEventListener implements AsyncOrchestrationEventHandler {
  private OutcomeService outcomeService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String orgId = AmbianceHelper.getOrgIdentifier(ambiance);
    String projectId = AmbianceHelper.getProjectIdentifier(ambiance);
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    String planExecutionId = ambiance.getPlanExecutionId();
    // TODO get deployedAt from ambiance

    createInfrastructureMappingIfNew(Ambiance ambiance);

    prepareDeploymentEvent(Ambiance ambiance);
  }

  // --------------------- PRIVATE METHODS ------------------------

  private InfrastructureMapping createInfrastructureMappingIfNew(Ambiance ambiance) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String orgId = AmbianceHelper.getOrgIdentifier(ambiance);
    String projectId = AmbianceHelper.getProjectIdentifier(ambiance);

    ServiceOutcome serviceOutcome = (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));

    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    K8sRollingOutcome k8sRollingOutcome = (K8sRollingOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_ROLL_OUT));

    /**
     * private unique id;
     * private String infraMappingType; // DIRECT_KUBERNETES
     *   private String connectorType;
     *   private String connectorId;
     *   private String deploymentType; //  HELM, KUBERNETES
     *   clustername
     *   namespace
     */

    String envId = infrastructureOutcome.getEnvironment().getIdentifier();
    String serviceId = serviceOutcome.getIdentifier();
    String releaseName = k8sRollingOutcome.getReleaseName();

    serviceOutcome

        infrastructureOutcome.getType();

    // Ask sahil to add field to determine which steps produce pods/deployment
    // Ask Sahil to maintain common constants file that provides infrastucture outcome type
    // use it to determine the type of infrastrucure mapping

    K8sGcpInfrastructureOutcome,
        K8sDirectInfrastructureOutcome

        return null;
  }

  private DeploymentEvent prepareDeploymentEvent(Ambiance ambiance) {
    ServiceOutcome serviceOutcome = (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));

    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    K8sRollingOutcome k8sRollingOutcome = (K8sRollingOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_ROLL_OUT));

    /**
     *   private String pipelineExecutionName;
     *   private String artifactId;
     *   private String artifactName;
     *   private String artifactBuildNum;
     *   private String deployedById;
     *   private String deployedByName;
     *   private long deployedAt;
     * private String namespace;
     *   private Set<String> namespaces = new HashSet<>();
     *   private String blueGreenStageColor;
     *
     *   there's list of artifacts - primary and sidecars, which one to use
     *
     *   // save both primary and sidecard artifacts
     */

    int releaseNumber = k8sRollingOutcome.getReleaseNumber();
    String pipelineExecutionId = ambiance.getPlanExecutionId();

    return null;
  }
}
