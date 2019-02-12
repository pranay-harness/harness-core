package io.harness.functional.automationVerification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.sm.StateType.HTTP;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.RestUtils.ArtifactRestUtil;
import io.harness.functional.RestUtils.WorkflowRestUtil;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.ArtifactStreamGenerator;
import io.harness.generator.ArtifactStreamGenerator.ArtifactStreams;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureMappingGenerator;
import io.harness.generator.InfrastructureMappingGenerator.InfrastructureMappings;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.states.HttpState;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class WorkflowWithRollbackTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ArtifactStreamGenerator artifactStreamGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;

  private Application application;
  private Service service;
  private Environment environment;
  private InfrastructureMapping infrastructureMapping;

  final Seed seed = new Seed(0);
  Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    resetCache();

    application = applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST);
    assertThat(application).isNotNull();

    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
    assertThat(environment).isNotNull();

    infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, InfrastructureMappings.AWS_SSH_FUNCTIONAL_TEST);
    assertThat(infrastructureMapping).isNotNull();

    service = serviceGenerator.ensurePredefined(seed, owners, Services.FUNCTIONAL_TEST);
    assertThat(service).isNotNull();

    ArtifactStream artifactStream =
        artifactStreamGenerator.ensurePredefined(seed, owners, ArtifactStreams.ARTIFACTORY_ECHO_WAR);
    assertThat(artifactStream).isNotNull();

    resetCache();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testWFWithRollback() {
    Workflow savedWorkflow = addWorkflow();
    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotNull();

    WorkflowPhase updatedPhase2 = addVerificationPhase2(savedWorkflow);
    assertThat(updatedPhase2).isNotNull();

    Artifact artifact = ArtifactRestUtil.getArtifact(application.getUuid(), environment.getUuid(), service.getUuid());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(savedWorkflow.getUuid());
    executionArgs.setArtifacts(Collections.singletonList(artifact));

    WorkflowExecution workflowExecution =
        WorkflowRestUtil.runWorkflow(application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();

    Awaitility.await()
        .atMost(120, TimeUnit.SECONDS)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> given()
                          .auth()
                          .oauth2(bearerToken)
                          .queryParam("appId", application.getUuid())
                          .get("/executions/" + workflowExecution.getUuid())
                          .jsonPath()
                          .<String>getJsonObject("resource.status")
                          .equals(ExecutionStatus.FAILED.name()));

    WorkflowExecution completedWorkflowExecution =
        workflowExecutionService.getExecutionDetails(application.getUuid(), workflowExecution.getUuid(), true, null);
    System.out.println("test");
    assertThat(completedWorkflowExecution.getExecutionNode().getStatus()).isEqualTo("SUCCESS");
    assertThat(completedWorkflowExecution.getExecutionNode().getNext().getStatus()).isEqualTo("SUCCESS");
    assertThat(completedWorkflowExecution.getExecutionNode().getNext().getNext().getStatus()).isEqualTo("FAILED");

    assertThat(WorkflowRestUtil.deleteWorkflow(savedWorkflow.getUuid(), application.getUuid())).isNull();
  }

  private WorkflowPhase addVerificationPhase2(Workflow savedWorkflow) {
    PhaseStep verifyPhaseStep = aPhaseStep(PhaseStepType.VERIFY_SERVICE, "verifyHttp").addStep(getHTTPNode()).build();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow();
    for (WorkflowPhase workflowPhase : orchestrationWorkflow.getWorkflowPhases()) {
      if (workflowPhase.getName().equalsIgnoreCase("Phase 2")) {
        for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
          if (phaseStep.getPhaseStepType().equals(PhaseStepType.VERIFY_SERVICE)) {
            phaseStep.setSteps(Collections.singletonList(getHTTPNode()));
            break;
          }
        }
        return WorkflowRestUtil.saveWorkflowPhase(
            application.getUuid(), savedWorkflow.getUuid(), workflowPhase.getUuid(), workflowPhase);
      }
    }
    return null;
  }

  private Workflow addWorkflow() {
    WorkflowPhase phase1 =
        aWorkflowPhase().serviceId(service.getUuid()).infraMappingId(infrastructureMapping.getUuid()).build();
    WorkflowPhase phase2 =
        aWorkflowPhase().serviceId(service.getUuid()).infraMappingId(infrastructureMapping.getUuid()).build();

    Workflow variableTestWorkflow =
        aWorkflow()
            .withName("Echo deployment with rollback")
            .withDescription("Echo deployment with Rollback")
            .withServiceId(service.getUuid())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withEnvId(environment.getUuid())
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow().withWorkflowPhases(ImmutableList.of(phase1, phase2)).build())
            .build();

    return WorkflowRestUtil.createWorkflow(AccountGenerator.ACCOUNT_ID, application.getUuid(), variableTestWorkflow);
  }

  private GraphNode getHTTPNode() {
    return GraphNode.builder()
        .id(generateUuid())
        .type(HTTP.name())
        .name("HTTP")
        .properties(ImmutableMap.<String, Object>builder()
                        .put(HttpState.URL_KEY, "failed")
                        .put(HttpState.METHOD_KEY, "GET")
                        .build())
        .build();
  }
}
