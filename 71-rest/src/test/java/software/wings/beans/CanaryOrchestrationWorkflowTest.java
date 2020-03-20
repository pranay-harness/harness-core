package software.wings.beans;

import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.PRASHANT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  // check variables metadata is correct when all env, srv, infra is templatised, templated pipelines is on/off
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicAllTemplatised() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE);

    TemplateExpression srvTemplateExpression =
        TemplateExpression.builder().expression("${Service}").fieldName("serviceId").metadata(srvMetadata).build();
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION);
    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .expression("${InfraDefinition_Kubernetes}")
                                                     .fieldName("infraDefinitionId")
                                                     .metadata(infraMetadata)
                                                     .build();

    Map<String, Object> envMetadata = getVarMetadata(ENVIRONMENT);
    TemplateExpression envTemplateExpression =
        TemplateExpression.builder().expression("${Environment}").fieldName("envId").metadata(envMetadata).build();

    Variable serviceVar = aVariable().entityType(SERVICE).name("Service").metadata(srvMetadata).build();
    Variable envVar = aVariable().entityType(ENVIRONMENT).name("Environment").metadata(envMetadata).build();
    Variable infraVar = aVariable()
                            .entityType(INFRASTRUCTURE_DEFINITION)
                            .name("InfraDefinition_Kubernetes")
                            .metadata(infraMetadata)
                            .build();

    List<Variable> workflowVariables = asList(serviceVar, envVar, infraVar);
    Workflow workflow =
        getWorkflow(asList(srvTemplateExpression, infraTemplateExpression), envTemplateExpression, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(true, true, workflow);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Environment");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service");
  }

  // check variables metadata is correct when all env, srv, infra is templatised, templated pipelines is on/off
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicAllTemplatisedFFOff() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE);

    TemplateExpression srvTemplateExpression =
        TemplateExpression.builder().expression("${Service}").fieldName("serviceId").metadata(srvMetadata).build();
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION);
    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .expression("${InfraDefinition_Kubernetes}")
                                                     .fieldName("infraDefinitionId")
                                                     .metadata(infraMetadata)
                                                     .build();

    Map<String, Object> envMetadata = getVarMetadata(ENVIRONMENT);
    TemplateExpression envTemplateExpression =
        TemplateExpression.builder().expression("${Environment}").fieldName("envId").metadata(envMetadata).build();

    Variable serviceVar = aVariable().entityType(SERVICE).name("Service").metadata(srvMetadata).build();
    Variable envVar = aVariable().entityType(ENVIRONMENT).name("Environment").metadata(envMetadata).build();
    Variable infraVar = aVariable()
                            .entityType(INFRASTRUCTURE_DEFINITION)
                            .name("InfraDefinition_Kubernetes")
                            .metadata(infraMetadata)
                            .build();

    List<Variable> workflowVariables = asList(serviceVar, envVar, infraVar);
    Workflow workflow =
        getWorkflow(asList(srvTemplateExpression, infraTemplateExpression), envTemplateExpression, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(true, false, workflow);

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Environment");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.RELATED_FIELD))
        .isNull();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(2).getMetadata().get(Variable.ENV_ID))
        .isNull();
  }

  // check variables metadata is correct when infra is templatised, templated pipelines is on/off
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicInfraTemplatised() {
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION);
    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .expression("${InfraDefinition_Kubernetes}")
                                                     .fieldName("infraDefinitionId")
                                                     .metadata(infraMetadata)
                                                     .build();

    Variable infraVar = aVariable()
                            .entityType(INFRASTRUCTURE_DEFINITION)
                            .name("InfraDefinition_Kubernetes")
                            .metadata(infraMetadata)
                            .build();

    List<Variable> workflowVariables = asList(infraVar);
    Workflow workflow = getWorkflow(asList(infraTemplateExpression), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(true, true, workflow);

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.SERVICE_ID))
        .isEqualTo(SERVICE_ID);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.ENV_ID))
        .isEqualTo(ENV_ID);
  }

  // check variables metadata is correct when all srv, infra is templatised, templated pipelines is on/off
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void checkOnLoadVariablesBasicSrvInfraTemplatised() {
    Map<String, Object> srvMetadata = getVarMetadata(SERVICE);

    TemplateExpression srvTemplateExpression =
        TemplateExpression.builder().expression("${Service}").fieldName("serviceId").metadata(srvMetadata).build();
    Map<String, Object> infraMetadata = getVarMetadata(INFRASTRUCTURE_DEFINITION);
    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .expression("${InfraDefinition_Kubernetes}")
                                                     .fieldName("infraDefinitionId")
                                                     .metadata(infraMetadata)
                                                     .build();

    Variable serviceVar = aVariable().entityType(SERVICE).name("Service").metadata(srvMetadata).build();
    Variable infraVar = aVariable()
                            .entityType(INFRASTRUCTURE_DEFINITION)
                            .name("InfraDefinition_Kubernetes")
                            .metadata(infraMetadata)
                            .build();

    List<Variable> workflowVariables = asList(serviceVar, infraVar);
    Workflow workflow = getWorkflow(asList(srvTemplateExpression, infraTemplateExpression), null, workflowVariables);

    workflow.getOrchestrationWorkflow().onLoad(true, true, workflow);

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables()).isNotEmpty();
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getName()).isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(0).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("InfraDefinition_Kubernetes");

    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getName())
        .isEqualTo("InfraDefinition_Kubernetes");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.RELATED_FIELD))
        .isEqualTo("Service");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.ENV_ID))
        .isEqualTo(ENV_ID);
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables().get(1).getMetadata().get(Variable.SERVICE_ID))
        .isNull();
  }
  @NotNull
  private Workflow getWorkflow(List<TemplateExpression> phaseTemplateExpression,
      TemplateExpression envTemplateExpression, List<Variable> workflowVariables) {
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .uuid("WORKFLOW_PHASE_ID")
                                      .infraMappingId(INFRA_MAPPING_ID)
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

  @NotNull
  private Map<String, Object> getVarMetadata(EntityType entityType) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("entityType", entityType);
    metadata.put("artifactType", "DOCKER");

    if (SERVICE == entityType) {
      metadata.put(Variable.RELATED_FIELD, "InfraDefinition_Kubernetes");
    }
    return metadata;
  }
}