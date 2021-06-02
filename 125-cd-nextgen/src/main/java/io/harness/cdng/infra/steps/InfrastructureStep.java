package io.harness.cdng.infra.steps;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.InfrastructureMapper;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.rbac.PrincipalTypeProtoToPrincipalTypeMapper;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rbac.CDNGRbacPermissions;
import io.harness.steps.EntityReferenceExtractorUtils;
import io.harness.steps.executable.SyncExecutableWithRbac;
import io.harness.walktree.visitor.SimpleVisitorFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.Set;

@OwnedBy(CDC)
public class InfrastructureStep implements SyncExecutableWithRbac<InfraStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.INFRASTRUCTURE.getName()).build();
  private static String INFRASTRUCTURE_COMMAND_UNIT = "Execute";

  @Inject private EnvironmentService environmentService;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private SimpleVisitorFactory simpleVisitorFactory;
  @Inject @Named("PRIVILEGED") private AccessControlClient accessControlClient;
  @Inject private EntityReferenceExtractorUtils entityReferenceExtractorUtils;
  @Inject private PipelineRbacHelper pipelineRbacHelper;

  @Override
  public Class<InfraStepParameters> getStepParametersClass() {
    return InfraStepParameters.class;
  }

  InfraMapping createInfraMappingObject(Infrastructure infrastructureSpec) {
    return infrastructureSpec.getInfraMapping();
  }

  @Override
  public StepResponse executeSyncAfterRbac(Ambiance ambiance, InfraStepParameters infraStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    long startTime = System.currentTimeMillis();
    NGLogCallback ngManagerLogCallback =
        new NGLogCallback(logStreamingStepClientFactory, ambiance, INFRASTRUCTURE_COMMAND_UNIT, true);
    ngManagerLogCallback.saveExecutionLog("Starting Infrastructure logs");
    PipelineInfrastructure pipelineInfrastructure = infraStepParameters.getPipelineInfrastructure();

    Infrastructure infraOverrides = null;
    if (pipelineInfrastructure.getUseFromStage() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides().getInfrastructureDefinition() != null) {
      infraOverrides = pipelineInfrastructure.getUseFromStage().getOverrides().getInfrastructureDefinition().getSpec();
    }

    Infrastructure infrastructure = pipelineInfrastructure.getInfrastructureDefinition().getSpec();
    Infrastructure finalInfrastructure =
        infraOverrides != null ? infrastructure.applyOverrides(infraOverrides) : infrastructure;
    validateInfrastructure(finalInfrastructure);
    EnvironmentOutcome environmentOutcome = (EnvironmentOutcome) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.ENVIRONMENT));
    InfrastructureOutcome infrastructureOutcome =
        InfrastructureMapper.toOutcome(finalInfrastructure, environmentOutcome);
    ngManagerLogCallback.saveExecutionLog(
        "Infrastructure Step completed", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder()
                         .outcome(infrastructureOutcome)
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .group(OutcomeExpressionConstants.INFRASTRUCTURE_GROUP)
                         .build())
        .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                                                        .setUnitName(INFRASTRUCTURE_COMMAND_UNIT)
                                                        .setStatus(UnitStatus.SUCCESS)
                                                        .setStartTime(startTime)
                                                        .setEndTime(System.currentTimeMillis())
                                                        .build()))
        .build();
  }

  @VisibleForTesting
  void validateInfrastructure(Infrastructure infrastructure) {
    if (infrastructure == null) {
      throw new InvalidRequestException("Infrastructure definition can't be null or empty");
    }
    switch (infrastructure.getKind()) {
      case InfrastructureKind.KUBERNETES_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        validateExpression(k8SDirectInfrastructure.getConnectorRef(), k8SDirectInfrastructure.getNamespace(),
            k8SDirectInfrastructure.getReleaseName());
        break;

      case InfrastructureKind.KUBERNETES_GCP:
        K8sGcpInfrastructure k8sGcpInfrastructure = (K8sGcpInfrastructure) infrastructure;
        validateExpression(k8sGcpInfrastructure.getConnectorRef(), k8sGcpInfrastructure.getNamespace(),
            k8sGcpInfrastructure.getReleaseName(), k8sGcpInfrastructure.getCluster());
        break;
      default:
        throw new InvalidArgumentsException(format("Unknown Infrastructure Kind : [%s]", infrastructure.getKind()));
    }
  }

  @SafeVarargs
  private final <T> void validateExpression(ParameterField<T>... inputs) {
    for (ParameterField<T> input : inputs) {
      if (!ParameterField.isNull(input) && input.isExpression()) {
        throw new InvalidRequestException(format("Unresolved Expression : [%s]", input.getExpressionValue()));
      }
    }
  }

  @Override
  public void validateResources(Ambiance ambiance, InfraStepParameters stepParameters) {
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    String principal = executionPrincipalInfo.getPrincipal();
    if (EmptyPredicate.isEmpty(principal)) {
      return;
    }
    PrincipalType principalType = PrincipalTypeProtoToPrincipalTypeMapper.convertToAccessControlPrincipalType(
        executionPrincipalInfo.getPrincipalType());
    Set<EntityDetailProtoDTO> entityDetails =
        entityReferenceExtractorUtils.extractReferredEntities(ambiance, stepParameters.getPipelineInfrastructure());
    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetails);
    if (stepParameters.getPipelineInfrastructure().getEnvironmentRef() == null
        || EmptyPredicate.isEmpty(stepParameters.getPipelineInfrastructure().getEnvironmentRef().getValue())) {
      accessControlClient.checkForAccessOrThrow(Principal.of(principalType, principal),
          ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of("ENVIRONMENT", null),
          CDNGRbacPermissions.ENVIRONMENT_CREATE_PERMISSION, "Validation for Infrastructure Step failed");
    }
  }
}
