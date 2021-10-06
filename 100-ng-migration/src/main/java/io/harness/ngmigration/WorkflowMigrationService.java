package io.harness.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.StepGroupElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.serializer.JsonUtils;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureType;
import io.harness.yaml.core.failurestrategy.OnFailureConfig;
import io.harness.yaml.core.failurestrategy.rollback.StageRollbackFailureActionConfig;
import io.harness.yaml.core.timeout.Timeout;

import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
import software.wings.ngmigration.NgMigration;
import software.wings.service.impl.yaml.handler.workflow.RollingWorkflowYamlHandler;
import software.wings.service.intfc.WorkflowService;
import software.wings.yaml.workflow.RollingWorkflowYaml;
import software.wings.yaml.workflow.StepYaml;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class WorkflowMigrationService implements NgMigration {
  @Inject private WorkflowService workflowService;
  @Inject private RollingWorkflowYamlHandler rollingWorkflowYamlHandler;
  @Inject private NgMigrationFactory ngMigrationFactory;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    Workflow workflow = (Workflow) entity;
    String entityId = workflow.getUuid();
    CgEntityId workflowEntityId = CgEntityId.builder().type(NGMigrationEntityType.WORKFLOW).id(entityId).build();
    CgEntityNode workflowNode = CgEntityNode.builder()
                                    .id(entityId)
                                    .type(NGMigrationEntityType.WORKFLOW)
                                    .entityId(workflowEntityId)
                                    .entity(workflow)
                                    .build();

    Set<CgEntityId> children = new HashSet<>();
    if (EmptyPredicate.isNotEmpty(workflow.getServices())) {
      children.addAll(
          workflow.getServices()
              .stream()
              .map(service -> CgEntityId.builder().type(NGMigrationEntityType.SERVICE).id(service.getUuid()).build())
              .collect(Collectors.toSet()));
    }
    OrchestrationWorkflowType orchestrationWorkflowType =
        workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType();
    if (!OrchestrationWorkflowType.ROLLING.equals(orchestrationWorkflowType)) {
      throw new UnsupportedOperationException("Currently only support rolling WF");
    }
    return DiscoveryNode.builder().children(children).entityNode(workflowNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(workflowService.readWorkflow(appId, entityId));
  }

  @Override
  public NGMigrationStatus canMigrate(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    return null;
  }

  @Override
  public void migrate(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {}

  public StageElementWrapperConfig getNgStage(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    Workflow workflow = (Workflow) entities.get(entityId).getEntity();
    RollingWorkflowYaml rollingWorkflowYaml = rollingWorkflowYamlHandler.toYaml(workflow, workflow.getAppId());
    List<ExecutionWrapperConfig> steps = new ArrayList<>();
    List<ExecutionWrapperConfig> rollingSteps = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(rollingWorkflowYaml.getPhases())) {
      List<WorkflowPhase.Yaml> phases = rollingWorkflowYaml.getPhases();
      steps.addAll(phases.stream()
                       .map(phase -> {
                         List<PhaseStep.Yaml> phaseSteps = phase.getPhaseSteps();
                         List<ExecutionWrapperConfig> currSteps = new ArrayList<>();
                         if (EmptyPredicate.isNotEmpty(phaseSteps)) {
                           currSteps = phaseSteps.stream()
                                           .filter(phaseStep -> EmptyPredicate.isNotEmpty(phaseStep.getSteps()))
                                           .flatMap(phaseStep -> phaseStep.getSteps().stream())
                                           .map(phaseStep -> {
                                             return ExecutionWrapperConfig.builder()
                                                 .step(JsonUtils.asTree(getStepElementConfig(phaseStep)))
                                                 .build();
                                           })
                                           .collect(Collectors.toList());
                         }
                         return ExecutionWrapperConfig.builder()
                             .stepGroup(JsonUtils.asTree(StepGroupElementConfig.builder()
                                                             .identifier(phase.getName())
                                                             .name(phase.getName())
                                                             .steps(currSteps)
                                                             .skipCondition(null)
                                                             .when(null)
                                                             .failureStrategies(null)
                                                             .build()))
                             .build();
                       })
                       .collect(Collectors.toList()));
    }

    return StageElementWrapperConfig.builder()
        .stage(JsonUtils.asTree(
            StageElementConfig.builder()
                .name(workflow.getName())
                .identifier(workflow.getName())
                .type("Deployment")
                .description(ParameterField.<String>builder().value(workflow.getDescription()).build())
                .failureStrategies(Collections.singletonList(
                    FailureStrategyConfig.builder()
                        .onFailure(OnFailureConfig.builder()
                                       .errors(Collections.singletonList(NGFailureType.ALL_ERRORS))
                                       .action(StageRollbackFailureActionConfig.builder().build())
                                       .build())
                        .build()))
                //                .stageType(
                //                    DeploymentStageConfig.builder()
                //                        .serviceConfig(ServiceConfig.builder().build())
                //                        .infrastructure(PipelineInfrastructure.builder().build())
                //                        .execution(ExecutionElementConfig.builder().steps(steps).rollbackSteps(rollingSteps).build())
                //                        .build())
                .build()))
        .parallel(null)
        .build();
  }

  @Override
  public List<NGYamlFile> getYamls(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    List<NGYamlFile> files = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(graph.get(entityId))) {
      graph.get(entityId).forEach(entityId1 -> {
        files.addAll(ngMigrationFactory.getMethod(entityId1.getType()).getYamls(entities, graph, entityId1));
      });
    }
    return files;
  }

  private StepElementConfig getStepElementConfig(StepYaml step) {
    if (step.getType().equals("K8S_DEPLOYMENT_ROLLING")) {
      Map<String, Object> properties =
          EmptyPredicate.isNotEmpty(step.getProperties()) ? step.getProperties() : new HashMap<>();
      return StepElementConfig.builder()
          .identifier(step.getName())
          .name(step.getName())
          .type(step.getType())
          //          .stepSpecType(K8sRollingStepInfo.infoBuilder()
          //                            .skipDryRun(ParameterField.<Boolean>builder().value(false).build())
          //                            .build())
          .timeout(ParameterField.<Timeout>builder()
                       .value(Timeout.builder()
                                  .timeoutString(properties.getOrDefault("stateTimeoutInMinutes", "10") + "m")
                                  .build())
                       .build())
          .build();
    }
    throw new UnsupportedOperationException("Not supported");
  }
}
