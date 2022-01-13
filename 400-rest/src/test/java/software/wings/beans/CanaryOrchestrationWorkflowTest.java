/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.PRABU;
import static io.harness.rule.OwnerRule.PRASHANT;

import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.ARTIFACT_STREAM;
import static software.wings.beans.EntityType.CF_AWS_CONFIG_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.GCP_CONFIG;
import static software.wings.beans.EntityType.GIT_CONFIG;
import static software.wings.beans.EntityType.HELM_GIT_CONFIG_ID;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.JENKINS_SERVER;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SPLUNK_CONFIGID;
import static software.wings.beans.EntityType.SS_SSH_CONNECTION_ATTRIBUTE;
import static software.wings.beans.EntityType.SS_WINRM_CONNECTION_ATTRIBUTE;
import static software.wings.beans.EntityType.SUMOLOGIC_CONFIGID;
import static software.wings.beans.EntityType.USER_GROUP;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class CanaryOrchestrationWorkflowTest extends WingsBaseTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void checkLastPhaseForOnDemandRollback() {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(aWorkflowPhase()
                                  .name("Phase 1")
                                  .infraMappingId(INFRA_MAPPING_ID)
                                  .serviceId(SERVICE_ID)
                                  .deploymentType(SSH)
                                  .build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    assertThat(canaryOrchestrationWorkflow.checkLastPhaseForOnDemandRollback("Staging Execution Phase 1")).isTrue();
    assertThat(canaryOrchestrationWorkflow.checkLastPhaseForOnDemandRollback("Staging Execution Phase 2")).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void checkLastPhaseForOnDemandRollbackNoPhases() {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    assertThat(canaryOrchestrationWorkflow.checkLastPhaseForOnDemandRollback("Staging Execution Phase 1")).isFalse();
  }

  // check variables metadata is correct when all env, infra is templatised, templated pipelines is on
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicEnvInfratemplatised() {
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());
    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .expression("${InfraDefinition_Kubernetes}")
                                                     .fieldName("infraDefinitionId")
                                                     .metadata(infraMetadata)
                                                     .build();

    Map<String, Object> envMetadata = getVarMetadata(ENVIRONMENT.toString());
    TemplateExpression envTemplateExpression =
        TemplateExpression.builder().expression("${Environment}").fieldName("envId").metadata(envMetadata).build();

    Variable envVar = getVariable("Environment", envMetadata);
    Variable infraVar = getVariable("InfraDefinition_Kubernetes", infraMetadata);

    List<Variable> workflowVariables = asList(envVar, infraVar);
    Workflow workflow = getBasicWorkflow(asList(infraTemplateExpression), envTemplateExpression, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Environment");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isNull();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.SERVICE_ID))
        .isEqualTo(SERVICE_ID);
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
  }

  // check variables metadata is correct when all env, srv, infra is templatised
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicAllTemplatised() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());

    TemplateExpression srvTemplateExpression =
        TemplateExpression.builder().expression("${Service}").fieldName("serviceId").metadata(srvMetadata).build();
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());
    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .expression("${InfraDefinition_Kubernetes}")
                                                     .fieldName("infraDefinitionId")
                                                     .metadata(infraMetadata)
                                                     .build();

    Map<String, Object> envMetadata = getVarMetadata(ENVIRONMENT.toString());
    TemplateExpression envTemplateExpression =
        TemplateExpression.builder().expression("${Environment}").fieldName("envId").metadata(envMetadata).build();

    Variable serviceVar = getVariable("Service", srvMetadata);
    Variable envVar = getVariable("Environment", envMetadata);
    Variable infraVar = getVariable("InfraDefinition_Kubernetes", infraMetadata);

    List<Variable> workflowVariables = asList(serviceVar, envVar, infraVar);
    Workflow workflow = getBasicWorkflow(
        asList(srvTemplateExpression, infraTemplateExpression), envTemplateExpression, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Environment");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.INFRA_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.ENV_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
  }

  // check variables metadata is correct when infra is templatised, templated pipelines is on
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicInfraTemplatised() {
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());
    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .expression("${InfraDefinition_Kubernetes}")
                                                     .fieldName("infraDefinitionId")
                                                     .metadata(infraMetadata)
                                                     .build();

    Variable infraVar = getVariable("InfraDefinition_Kubernetes", infraMetadata);

    List<Variable> workflowVariables = asList(infraVar);
    Workflow workflow = getBasicWorkflow(asList(infraTemplateExpression), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.SERVICE_ID))
        .isEqualTo(SERVICE_ID);
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.ENV_ID))
        .isEqualTo(ENV_ID);
  }

  // check variables metadata is correct when srv is templatised, templated pipelines is on
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicServiceTemplatised() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());

    TemplateExpression srvTemplateExpression =
        TemplateExpression.builder().expression("${Service}").fieldName("serviceId").metadata(srvMetadata).build();

    Variable serviceVar = getVariable("Service", srvMetadata);

    List<Variable> workflowVariables = asList(serviceVar);
    Workflow workflow = getBasicWorkflow(asList(srvTemplateExpression), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.INFRA_ID))
        .isEqualTo(INFRA_DEFINITION_ID);
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
  }

  // check variables metadata is correct when srv, infra is templatised, templated pipelines is on
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicSrvInfraTemplatised() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());

    TemplateExpression srvTemplateExpression =
        TemplateExpression.builder().expression("${Service}").fieldName("serviceId").metadata(srvMetadata).build();
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());
    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .expression("${InfraDefinition_Kubernetes}")
                                                     .fieldName("infraDefinitionId")
                                                     .metadata(infraMetadata)
                                                     .build();

    Variable serviceVar = getVariable("Service", srvMetadata);
    Variable infraVar = getVariable("InfraDefinition_Kubernetes", infraMetadata);

    List<Variable> workflowVariables = asList(serviceVar, infraVar);
    Workflow workflow =
        getBasicWorkflow(asList(srvTemplateExpression, infraTemplateExpression), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.INFRA_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service");
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.ENV_ID))
        .isEqualTo(ENV_ID);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
  }

  // check variables metadata is correct when all env, srv, infra is templatised, templated pipelines is on
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesMultiServiceAllTemplatised() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());
    Map<String, Object> envMetadata = getVarMetadata(ENVIRONMENT.toString());

    TemplateExpression srvTemplateExpressionPhase1 = getTemplateExpression("${Service}", "serviceId", srvMetadata);
    TemplateExpression srvTemplateExpressionPhase2 = getTemplateExpression("${Service2}", "serviceId", srvMetadata);

    TemplateExpression infraTemplateExpressionPhase1 =
        getTemplateExpression("${InfraDefinition_Kubernetes}", "infraDefinitionId", infraMetadata);
    TemplateExpression infraTemplateExpressionPhase2 =
        getTemplateExpression("${InfraDefinition_Kubernetes2}", "infraDefinitionId", infraMetadata);

    TemplateExpression envTemplateExpression = getTemplateExpression("${Environment}", "envId", envMetadata);

    Variable serviceVarPhase1 = getVariable("Service", srvMetadata);
    Variable serviceVarPhase2 = getVariable("Service2", srvMetadata);

    Variable envVar = getVariable("Environment", envMetadata);

    Variable infraVarPhase1 = getVariable("InfraDefinition_Kubernetes", infraMetadata);
    Variable infraVarPhase2 = getVariable("InfraDefinition_Kubernetes2", infraMetadata);

    List<Variable> workflowVariables =
        asList(serviceVarPhase1, serviceVarPhase2, envVar, infraVarPhase1, infraVarPhase2);
    WorkflowPhase phase1 = getWorkflowPhase(asList(srvTemplateExpressionPhase1, infraTemplateExpressionPhase1));
    WorkflowPhase phase2 = getWorkflowPhase(asList(srvTemplateExpressionPhase2, infraTemplateExpressionPhase2));

    Workflow workflow = getWorkflow(asList(phase1, phase2), envTemplateExpression, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Environment");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes,InfraDefinition_Kubernetes2");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.INFRA_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getName()).isEqualTo("Service2");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes2");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.INFRA_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(3).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(3).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(3).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(3).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(4).getName())
        .isEqualTo("InfraDefinition_Kubernetes2");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(4).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service2");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(4).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(4).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
  }

  // check variables metadata is correct when  srv, infra is templatised, same srv var, diff infra var, same deployment
  // type templated pipelines is on
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesMultiServiceSameSrvVarInfraTemplatised() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());

    TemplateExpression srvTemplateExpression = getTemplateExpression("${Service}", "serviceId", srvMetadata);

    TemplateExpression infraTemplateExpressionPhase1 =
        getTemplateExpression("${InfraDefinition_Kubernetes}", "infraDefinitionId", infraMetadata);
    TemplateExpression infraTemplateExpressionPhase2 =
        getTemplateExpression("${InfraDefinition_Kubernetes2}", "infraDefinitionId", infraMetadata);

    Variable serviceVar = getVariable("Service", srvMetadata);

    Variable infraVarPhase1 = getVariable("InfraDefinition_Kubernetes", infraMetadata);
    Variable infraVarPhase2 = getVariable("InfraDefinition_Kubernetes2", infraMetadata);

    List<Variable> workflowVariables = asList(serviceVar, infraVarPhase1, infraVarPhase2);
    WorkflowPhase phase1 = getWorkflowPhase(asList(srvTemplateExpression, infraTemplateExpressionPhase1));
    WorkflowPhase phase2 = getWorkflowPhase(asList(srvTemplateExpression, infraTemplateExpressionPhase2));

    Workflow workflow = getWorkflow(asList(phase1, phase2), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes,InfraDefinition_Kubernetes2");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.INFRA_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getName())
        .isEqualTo("InfraDefinition_Kubernetes2");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
  }

  // check variables metadata is correct when  srv, infra is templatised, same srv var,diff deployment type, infra non
  // templatised templated pipelines is on
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkVariablesMultiServiceSameSrvDiffDeploymentType() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());

    TemplateExpression srvTemplateExpression = getTemplateExpression("${Service}", "serviceId", srvMetadata);

    Variable serviceVar = getVariable("Service", srvMetadata);

    List<Variable> workflowVariables = asList(serviceVar);
    WorkflowPhase phase1 = getWorkflowPhase(asList(srvTemplateExpression));
    WorkflowPhase phase2 = getWorkflowPhase(asList(srvTemplateExpression));
    phase2.setDeploymentType(KUBERNETES);

    Workflow workflow = getWorkflow(asList(phase1, phase2), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.INFRA_ID))
        .isEqualTo(INFRA_DEFINITION_ID);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH,KUBERNETES");
  }

  // check variables metadata is correct when  srv, infra is templatised, same srv var,diff deployment type, infra non
  // templatised templated pipelines is on
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkVariablesMultiServiceSameSrvDiffDeploymentTypeDiffInfraId() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());

    TemplateExpression srvTemplateExpression = getTemplateExpression("${Service}", "serviceId", srvMetadata);

    Variable serviceVar = getVariable("Service", srvMetadata);

    List<Variable> workflowVariables = asList(serviceVar);
    WorkflowPhase phase1 = getWorkflowPhase(asList(srvTemplateExpression));
    WorkflowPhase phase2 = getWorkflowPhase(asList(srvTemplateExpression));
    phase2.setDeploymentType(KUBERNETES);
    phase2.setInfraDefinitionId("INFRA_DEFINITION_ID1");

    Workflow workflow = getWorkflow(asList(phase1, phase2), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.INFRA_ID))
        .isEqualTo("INFRA_DEFINITION_ID,INFRA_DEFINITION_ID1");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH,KUBERNETES");
  }

  // check variables metadata is correct when all srv, infra is templatised, same infra var, diff srv var, same
  // deployment type.
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesMultiServiceSameInfraVar() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());

    TemplateExpression srvTemplateExpressionPhase1 = getTemplateExpression("${Service}", "serviceId", srvMetadata);
    TemplateExpression srvTemplateExpressionPhase2 = getTemplateExpression("${Service2}", "serviceId", srvMetadata);

    TemplateExpression infraTemplateExpression =
        getTemplateExpression("${InfraDefinition_Kubernetes}", "infraDefinitionId", infraMetadata);

    Variable serviceVarPhase1 = getVariable("Service", srvMetadata);
    Variable serviceVarPhase2 = getVariable("Service2", srvMetadata);

    Variable infraVar = getVariable("InfraDefinition_Kubernetes", infraMetadata);

    List<Variable> workflowVariables = asList(serviceVarPhase1, serviceVarPhase2, infraVar);
    WorkflowPhase phase1 = getWorkflowPhase(asList(srvTemplateExpressionPhase1, infraTemplateExpression));
    WorkflowPhase phase2 = getWorkflowPhase(asList(srvTemplateExpressionPhase2, infraTemplateExpression));

    Workflow workflow = getWorkflow(asList(phase1, phase2), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.INFRA_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName()).isEqualTo("Service2");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.INFRA_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service,Service2");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
  }

  // check variables metadata is correct when all infra is templatised, same infra var, same deployment type.
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesMultiServiceSameInfraVarServiceNonTemplat() {
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());

    TemplateExpression infraTemplateExpression =
        getTemplateExpression("${InfraDefinition_Kubernetes}", "infraDefinitionId", infraMetadata);

    Variable infraVar = getVariable("InfraDefinition_Kubernetes", infraMetadata);

    List<Variable> workflowVariables = asList(infraVar);
    WorkflowPhase phase1 = getWorkflowPhase(asList(infraTemplateExpression));
    WorkflowPhase phase2 = getWorkflowPhase(asList(infraTemplateExpression));
    phase2.setServiceId("SERVICE_ID2");

    Workflow workflow = getWorkflow(asList(phase1, phase2), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isNull();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.SERVICE_ID))
        .isEqualTo("SERVICE_ID,SERVICE_ID2");
    assertThat(
        workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.DEPLOYMENT_TYPE))
        .isEqualTo("SSH");
  }

  // check variables metadata is correct when all infra is templatised, same infra var, diff deployment type.
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesMultiServiceSameInfraVarServiceNonTemplatDiffDT() {
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());

    TemplateExpression infraTemplateExpression =
        getTemplateExpression("${InfraDefinition_Kubernetes}", "infraDefinitionId", infraMetadata);

    Variable infraVar = getVariable("InfraDefinition_Kubernetes", infraMetadata);

    List<Variable> workflowVariables = asList(infraVar);
    WorkflowPhase phase1 = getWorkflowPhase(asList(infraTemplateExpression));
    WorkflowPhase phase2 = getWorkflowPhase(asList(infraTemplateExpression));
    phase2.setDeploymentType(KUBERNETES);

    Workflow workflow = getWorkflow(asList(phase1, phase2), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(workflow);
    assertThat(workflow.getOrchestrationWorkflow().isValid()).isFalse();
    assertThat(workflow.getOrchestrationWorkflow().getValidationMessage())
        .isEqualTo(
            "Cannot use same variable ${InfraDefinition_Kubernetes} for different deployment Types: Secure Shell (SSH), Kubernetes");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkTransientFields() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE.toString());
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION.toString());
    Map<String, Object> envMetadata = getVarMetadata(ENVIRONMENT.toString());

    TemplateExpression infraTemplateExpression =
        getTemplateExpression("${InfraDefinition_Kubernetes}", "infraDefinitionId", infraMetadata);

    TemplateExpression srvTemplateExpression = getTemplateExpression("${Service}", "serviceId", srvMetadata);
    TemplateExpression envTemplateExpression = getTemplateExpression("${Environment}", "envId", envMetadata);

    WorkflowPhase phase1 = getWorkflowPhase(asList(infraTemplateExpression, srvTemplateExpression));

    Workflow workflow = getWorkflow(asList(phase1), envTemplateExpression, null);

    workflow.getOrchestrationWorkflow().setTransientFields(workflow);
    assertThat(workflow.isEnvTemplatized()).isTrue();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    List<String> workflowPhaseIds = canaryOrchestrationWorkflow.getWorkflowPhaseIds();
    for (String id : workflowPhaseIds) {
      WorkflowPhase workflowPhase = canaryOrchestrationWorkflow.getWorkflowPhaseIdMap().get(id);
      WorkflowPhase rollbackPhase = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(id);

      assertThat(workflowPhase.isInfraTemplatised()).isTrue();
      assertThat(workflowPhase.isSrvTemplatised()).isTrue();
      assertThat(rollbackPhase.isInfraTemplatised()).isTrue();
      assertThat(rollbackPhase.isSrvTemplatised()).isTrue();
    }
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkTransientFieldsFalse() {
    WorkflowPhase phase1 = getWorkflowPhase(new ArrayList<>());

    Workflow workflow = getWorkflow(asList(phase1), null, null);

    workflow.getOrchestrationWorkflow().setTransientFields(workflow);
    assertThat(workflow.isEnvTemplatized()).isFalse();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    List<String> workflowPhaseIds = canaryOrchestrationWorkflow.getWorkflowPhaseIds();
    for (String id : workflowPhaseIds) {
      WorkflowPhase workflowPhase = canaryOrchestrationWorkflow.getWorkflowPhaseIdMap().get(id);
      WorkflowPhase rollbackPhase = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(id);

      assertThat(workflowPhase.isInfraTemplatised()).isFalse();
      assertThat(workflowPhase.isSrvTemplatised()).isFalse();
      assertThat(rollbackPhase.isInfraTemplatised()).isFalse();
      assertThat(rollbackPhase.isSrvTemplatised()).isFalse();
    }
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldAddTemplatizedVariables() {
    List<EntityType> entityTypes =
        asList(CF_AWS_CONFIG_ID, HELM_GIT_CONFIG_ID, SUMOLOGIC_CONFIGID, SPLUNK_CONFIGID, SS_SSH_CONNECTION_ATTRIBUTE,
            SS_WINRM_CONNECTION_ATTRIBUTE, USER_GROUP, GCP_CONFIG, GIT_CONFIG, JENKINS_SERVER, ARTIFACT_STREAM);
    List<Variable> userVariables =
        entityTypes.stream()
            .map(entityType
                -> aVariable().name(entityType.name()).type(VariableType.ENTITY).entityType(entityType).build())
            .collect(Collectors.toList());
    List<Variable> finalUserVariables = CanaryOrchestrationWorkflow.reorderUserVariables(userVariables);
    assertThat(finalUserVariables.stream().map(Variable::obtainEntityType).collect(Collectors.toList()))
        .containsExactly(CF_AWS_CONFIG_ID, HELM_GIT_CONFIG_ID, SUMOLOGIC_CONFIGID, SPLUNK_CONFIGID,
            SS_SSH_CONNECTION_ATTRIBUTE, SS_WINRM_CONNECTION_ATTRIBUTE, USER_GROUP, GCP_CONFIG, GIT_CONFIG,
            JENKINS_SERVER, ARTIFACT_STREAM);
  }

  private TemplateExpression getTemplateExpression(String expression, String fieldName, Map<String, Object> metadata) {
    return TemplateExpression.builder()
        .expression(expression)
        .fieldName(fieldName)
        .metadata(new HashMap<>(metadata))
        .build();
  }

  private Variable getVariable(String name, Map<String, Object> metadata) {
    return aVariable().type(VariableType.ENTITY).name(name).metadata(new HashMap<>(metadata)).build();
  }

  private Workflow getBasicWorkflow(List<TemplateExpression> phaseTemplateExpression,
      TemplateExpression envTemplateExpression, List<Variable> workflowVariables) {
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .uuid("WORKFLOW_PHASE_ID")
                                      .infraMappingId(INFRA_MAPPING_ID)
                                      .infraDefinitionId(INFRA_DEFINITION_ID)
                                      .serviceId(SERVICE_ID)
                                      .deploymentType(SSH)
                                      .templateExpressions(phaseTemplateExpression)
                                      .build();

    Workflow workflow =
        aWorkflow()
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                       .addWorkflowPhase(workflowPhase)
                                       .withWorkflowPhaseIdMap(ImmutableMap.of("WORKFLOW_PHASE_ID", workflowPhase))
                                       .withWorkflowPhaseIds(asList("WORKFLOW_PHASE_ID"))
                                       .withUserVariables(workflowVariables)
                                       .build())
            .envId(ENV_ID)
            .build();

    if (envTemplateExpression != null) {
      workflow.setTemplateExpressions(asList(envTemplateExpression));
    }
    return workflow;
  }

  private WorkflowPhase getWorkflowPhase(List<TemplateExpression> phaseTemplateExpression) {
    return aWorkflowPhase()
        .uuid("WORKFLOW_PHASE_ID")
        .infraMappingId(INFRA_MAPPING_ID)
        .infraDefinitionId(INFRA_DEFINITION_ID)
        .serviceId(SERVICE_ID)
        .deploymentType(SSH)
        .templateExpressions(phaseTemplateExpression)
        .build();
  }

  @NotNull
  private Workflow getWorkflow(
      List<WorkflowPhase> workflowPhases, TemplateExpression envTemplateExpression, List<Variable> workflowVariables) {
    CanaryOrchestrationWorkflowBuilder orchestrationWorkflowBuilder =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .withUserVariables(workflowVariables);

    Map<String, WorkflowPhase> workflowPhaseIdmap = new HashMap<>();
    Map<String, WorkflowPhase> rollbackPhaseIdmap = new HashMap<>();

    List<String> workflowPhaseIds = new ArrayList<>();
    int i = 1;
    for (WorkflowPhase workflowPhase : workflowPhases) {
      orchestrationWorkflowBuilder.addWorkflowPhase(workflowPhase);
      workflowPhaseIdmap.put("WORKFLOW_PHASE_ID" + i, workflowPhase);
      rollbackPhaseIdmap.put("WORKFLOW_PHASE_ID" + i, workflowPhase);
      workflowPhaseIds.add("WORKFLOW_PHASE_ID" + i);
      i++;
    }

    orchestrationWorkflowBuilder.withWorkflowPhaseIdMap(workflowPhaseIdmap);
    orchestrationWorkflowBuilder.withWorkflowPhaseIds(workflowPhaseIds);
    orchestrationWorkflowBuilder.withRollbackWorkflowPhaseIdMap(rollbackPhaseIdmap);
    Workflow workflow = aWorkflow().orchestrationWorkflow(orchestrationWorkflowBuilder.build()).envId(ENV_ID).build();
    if (envTemplateExpression != null) {
      workflow.setTemplateExpressions(asList(envTemplateExpression));
    }

    return workflow;
  }

  @NotNull
  private Map<String, Object> getVarMetadata(String entityType) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("entityType", entityType);
    metadata.put("artifactType", "DOCKER");

    if ("SERVICE".equals(entityType)) {
      metadata.put(Variable.RELATED_FIELD, "InfraDefinition_Kubernetes");
    }
    return metadata;
  }
}
