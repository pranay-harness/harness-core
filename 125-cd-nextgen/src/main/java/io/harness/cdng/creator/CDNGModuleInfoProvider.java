package io.harness.cdng.creator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.steps.ArtifactsStep;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.executions.beans.CDPipelineModuleInfo;
import io.harness.cdng.pipeline.executions.beans.CDPipelineModuleInfo.CDPipelineModuleInfoBuilder;
import io.harness.cdng.pipeline.executions.beans.CDStageModuleInfo;
import io.harness.cdng.pipeline.executions.beans.CDStageModuleInfo.CDStageModuleInfoBuilder;
import io.harness.cdng.pipeline.executions.beans.InfraExecutionSummary;
import io.harness.cdng.service.steps.ServiceConfigStep;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngpipeline.artifact.bean.ArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.ArtifactsOutcome;
import io.harness.ngpipeline.pipeline.executions.beans.ServiceExecutionSummary;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class CDNGModuleInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Inject OutcomeService outcomeService;

  public ServiceExecutionSummary.ArtifactsSummary mapArtifactsOutcomeToSummary(
      Optional<ArtifactsOutcome> artifactsOutcomeOptional) {
    ServiceExecutionSummary.ArtifactsSummary.ArtifactsSummaryBuilder artifactsSummaryBuilder =
        ServiceExecutionSummary.ArtifactsSummary.builder();
    if (artifactsOutcomeOptional == null || !artifactsOutcomeOptional.isPresent()) {
      return artifactsSummaryBuilder.build();
    }

    ArtifactsOutcome artifactsOutcome = artifactsOutcomeOptional.get();
    if (artifactsOutcome.getPrimary() != null) {
      artifactsSummaryBuilder.primary(artifactsOutcome.getPrimary().getArtifactSummary());
    }

    if (EmptyPredicate.isNotEmpty(artifactsOutcome.getSidecars())) {
      artifactsSummaryBuilder.sidecars(artifactsOutcome.getSidecars()
                                           .values()
                                           .stream()
                                           .filter(Objects::nonNull)
                                           .map(ArtifactOutcome::getArtifactSummary)
                                           .collect(Collectors.toList()));
    }

    return artifactsSummaryBuilder.build();
  }

  private Optional<ServiceStepOutcome> getServiceStepOutcome(NodeExecutionProto nodeExecutionProto) {
    OptionalOutcome optionalOutcome = outcomeService.resolveOptional(
        nodeExecutionProto.getAmbiance(), RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
    if (!optionalOutcome.isFound()) {
      return Optional.empty();
    }
    return Optional.ofNullable((ServiceStepOutcome) optionalOutcome.getOutcome());
  }

  private Optional<ArtifactsOutcome> getArtifactsOutcome(NodeExecutionProto nodeExecutionProto) {
    OptionalOutcome optionalOutcome = outcomeService.resolveOptional(
        nodeExecutionProto.getAmbiance(), RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.ARTIFACTS));
    if (!optionalOutcome.isFound()) {
      return Optional.empty();
    }
    return Optional.ofNullable((ArtifactsOutcome) optionalOutcome.getOutcome());
  }

  private Optional<EnvironmentOutcome> getEnvironmentOutcome(NodeExecutionProto nodeExecutionProto) {
    return outcomeService
        .findAllByRuntimeId(nodeExecutionProto.getAmbiance().getPlanExecutionId(), nodeExecutionProto.getUuid())
        .stream()
        .filter(outcome -> outcome instanceof InfrastructureOutcome)
        .map(outcome -> ((InfrastructureOutcome) outcome).getEnvironment())
        .findFirst();
  }

  private boolean isServiceNodeAndCompleted(PlanNodeProto node, Status status) {
    return Objects.equals(node.getStepType(), ServiceConfigStep.STEP_TYPE) && status == Status.SUCCEEDED;
  }

  private boolean isArtifactsNodeAndCompleted(PlanNodeProto node, Status status) {
    return Objects.equals(node.getStepType(), ArtifactsStep.STEP_TYPE) && status == Status.SUCCEEDED;
  }

  private boolean isInfrastructureNodeAndCompleted(PlanNodeProto node, Status status) {
    return Objects.equals(node.getStepType(), InfrastructureStep.STEP_TYPE) && status == Status.SUCCEEDED;
  }

  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    CDPipelineModuleInfoBuilder cdPipelineModuleInfoBuilder = CDPipelineModuleInfo.builder();
    if (isServiceNodeAndCompleted(nodeExecutionProto.getNode(), nodeExecutionProto.getStatus())) {
      Optional<ServiceStepOutcome> serviceOutcome = getServiceStepOutcome(nodeExecutionProto);
      serviceOutcome.ifPresent(outcome
          -> cdPipelineModuleInfoBuilder.serviceDefinitionType(outcome.getServiceDefinitionType())
                 .serviceIdentifier(outcome.getIdentifier()));
    }
    if (isInfrastructureNodeAndCompleted(nodeExecutionProto.getNode(), nodeExecutionProto.getStatus())) {
      List<Outcome> outcomes = outcomeService.fetchOutcomes(nodeExecutionProto.getOutcomeRefsList()
                                                                .stream()
                                                                .map(StepOutcomeRef::getInstanceId)
                                                                .collect(Collectors.toList()));
      for (Outcome outcome : outcomes) {
        if (outcome instanceof InfrastructureOutcome) {
          InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcome;
          cdPipelineModuleInfoBuilder.envIdentifier(infrastructureOutcome.getEnvironment().getIdentifier())
              .environmentType(infrastructureOutcome.getEnvironment().getType())
              .infrastructureType(infrastructureOutcome.getKind());
        }
      }
    }
    return cdPipelineModuleInfoBuilder.build();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    CDStageModuleInfoBuilder cdStageModuleInfoBuilder = CDStageModuleInfo.builder();
    if (isServiceNodeAndCompleted(nodeExecutionProto.getNode(), nodeExecutionProto.getStatus())) {
      Optional<ServiceStepOutcome> serviceOutcome = getServiceStepOutcome(nodeExecutionProto);
      Optional<ArtifactsOutcome> artifactsOutcome = getArtifactsOutcome(nodeExecutionProto);
      serviceOutcome.ifPresent(outcome
          -> cdStageModuleInfoBuilder.serviceInfo(ServiceExecutionSummary.builder()
                                                      .identifier(outcome.getIdentifier())
                                                      .displayName(outcome.getName())
                                                      .deploymentType(outcome.getServiceDefinitionType())
                                                      .artifacts(mapArtifactsOutcomeToSummary(artifactsOutcome))
                                                      .build()));
    }
    if (isInfrastructureNodeAndCompleted(nodeExecutionProto.getNode(), nodeExecutionProto.getStatus())) {
      Optional<EnvironmentOutcome> environmentOutcome = getEnvironmentOutcome(nodeExecutionProto);
      environmentOutcome.ifPresent(outcome
          -> cdStageModuleInfoBuilder.infraExecutionSummary(InfraExecutionSummary.builder()
                                                                .identifier(outcome.getIdentifier())
                                                                .name(outcome.getName())
                                                                .type(outcome.getType().name())
                                                                .build()));
    }
    return cdStageModuleInfoBuilder.build();
  }
}
