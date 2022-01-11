/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import static software.wings.ngmigration.NGMigrationEntityType.ENVIRONMENT;
import static software.wings.ngmigration.NGMigrationEntityType.INFRA;
import static software.wings.ngmigration.NGMigrationEntityType.SERVICE;
import static software.wings.ngmigration.NGMigrationEntityType.WORKFLOW;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureDef;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.StepGroupElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureType;
import io.harness.yaml.core.failurestrategy.OnFailureConfig;
import io.harness.yaml.core.failurestrategy.rollback.StageRollbackFailureActionConfig;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.utils.JsonPipelineUtils;

import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
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
  @Inject private InfraMigrationService infraMigrationService;
  @Inject private EnvironmentMigrationService environmentMigrationService;
  @Inject private ServiceMigrationService serviceMigrationService;
  @Inject private WorkflowService workflowService;
  @Inject private RollingWorkflowYamlHandler rollingWorkflowYamlHandler;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    Workflow workflow = (Workflow) entity;
    String entityId = workflow.getUuid();
    CgEntityId workflowEntityId = CgEntityId.builder().type(WORKFLOW).id(entityId).build();
    CgEntityNode workflowNode =
        CgEntityNode.builder().id(entityId).type(WORKFLOW).entityId(workflowEntityId).entity(workflow).build();

    Set<CgEntityId> children = new HashSet<>();
    if (EmptyPredicate.isNotEmpty(workflow.getServices())) {
      children.addAll(workflow.getServices()
                          .stream()
                          .map(service -> CgEntityId.builder().type(SERVICE).id(service.getUuid()).build())
                          .collect(Collectors.toSet()));
    }

    if (EmptyPredicate.isNotEmpty(workflow.getEnvId())) {
      children.add(CgEntityId.builder().type(ENVIRONMENT).id(workflow.getEnvId()).build());
    }
    if (EmptyPredicate.isNotEmpty(workflow.getInfraDefinitionId())) {
      children.add(CgEntityId.builder().type(INFRA).id(workflow.getInfraDefinitionId()).build());
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

  public StageElementWrapperConfig getNgStage(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NgEntityDetail> migratedEntities) {
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
                                                 .step(JsonPipelineUtils.asTree(getStepElementConfig(phaseStep)))
                                                 .build();
                                           })
                                           .collect(Collectors.toList());
                         }
                         return ExecutionWrapperConfig.builder()
                             .stepGroup(JsonPipelineUtils.asTree(
                                 StepGroupElementConfig.builder()
                                     .identifier(MigratorUtility.generateIdentifier(phase.getName()))
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

    ServiceConfig serviceConfig = null;
    if (EmptyPredicate.isNotEmpty(workflow.getOrchestration().getServiceIds())) {
      String serviceId = workflow.getOrchestration().getServiceIds().get(0);
      serviceConfig = serviceMigrationService.getServiceConfig(
          inputDTO, entities, graph, CgEntityId.builder().type(SERVICE).id(serviceId).build(), migratedEntities);
    }
    EnvironmentYaml environmentYaml = environmentMigrationService.getEnvironmentYaml(
        inputDTO, entities, graph, CgEntityId.builder().type(ENVIRONMENT).id(workflow.getEnvId()).build());

    InfrastructureDef infrastructureDef = null;
    if (EmptyPredicate.isNotEmpty(workflow.getOrchestration().getInfraDefinitionIds())) {
      String infraId = workflow.getOrchestration().getInfraDefinitionIds().get(0);
      infrastructureDef = infraMigrationService.getInfraDef(
          inputDTO, entities, graph, CgEntityId.builder().type(INFRA).id(infraId).build(), migratedEntities);
    }

    return StageElementWrapperConfig.builder()
        .stage(JsonPipelineUtils.asTree(
            StageElementConfig.builder()
                .name(workflow.getName())
                .identifier(MigratorUtility.generateIdentifier(workflow.getName()))
                .type("Deployment")
                .failureStrategies(Collections.singletonList(
                    FailureStrategyConfig.builder()
                        .onFailure(OnFailureConfig.builder()
                                       .errors(Collections.singletonList(NGFailureType.ALL_ERRORS))
                                       .action(StageRollbackFailureActionConfig.builder().build())
                                       .build())
                        .build()))
                .stageType(
                    DeploymentStageConfig.builder()
                        .serviceConfig(serviceConfig)
                        .infrastructure(PipelineInfrastructure.builder()
                                            .environment(environmentYaml)
                                            .infrastructureDefinition(infrastructureDef)
                                            .build())
                        .execution(ExecutionElementConfig.builder().steps(steps).rollbackSteps(rollingSteps).build())
                        .build())
                .build()))
        .parallel(null)
        .build();
  }

  @Override
  public List<NGYamlFile> getYamls(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NgEntityDetail> migratedEntities) {
    return new ArrayList<>();
  }

  private StepElementConfig getStepElementConfig(StepYaml step) {
    if (step.getType().equals("K8S_DEPLOYMENT_ROLLING")) {
      Map<String, Object> properties =
          EmptyPredicate.isNotEmpty(step.getProperties()) ? step.getProperties() : new HashMap<>();
      return StepElementConfig.builder()
          .identifier(MigratorUtility.generateIdentifier(step.getName()))
          .name(step.getName())
          .type(StepSpecTypeConstants.K8S_ROLLING_DEPLOY)
          .stepSpecType(K8sRollingStepInfo.infoBuilder().skipDryRun(ParameterField.createValueField(false)).build())
          .timeout(ParameterField.createValueField(
              Timeout.builder().timeoutString(properties.getOrDefault("stateTimeoutInMinutes", "10") + "m").build()))
          .build();
    }
    throw new UnsupportedOperationException("Not supported");
  }
}
