package io.harness.functional.pipelines;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.rule.OwnerRule.SUNIL;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureMappingGenerator;
import io.harness.generator.InfrastructureMappingGenerator.InfrastructureMappings;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.PipelineUtils;
import io.harness.testframework.framework.utils.WorkflowUtils;
import io.harness.testframework.restutils.ArtifactRestUtils;
import io.harness.testframework.restutils.ExecutionRestUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PipelineE2ETest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ArtifactStreamManager artifactStreamManager;

  private Application application;
  private Service service;
  private Environment environment;
  private InfrastructureMapping infrastructureMapping;
  private Workflow savedWorkflow;
  final Seed seed = new Seed(0);
  final String dummyWorkflow = "Dummy HTTP Workflow";
  String pipelineName = "";
  Owners owners;
  final int MAX_RETRIES = 60;
  final int DELAY_IN_MS = 4000;
  final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  Artifact artifact = null;

  WorkflowUtils wfUtils = new WorkflowUtils();

  @Before
  public void createAllEntities() {
    owners = ownerManager.create();

    application = applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST);
    assertThat(application).isNotNull();

    service = serviceGenerator.ensurePredefined(seed, owners, Services.FUNCTIONAL_TEST);
    assertThat(service).isNotNull();

    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
    assertThat(environment).isNotNull();

    infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, InfrastructureMappings.AWS_SSH_FUNCTIONAL_TEST);
    assertThat(infrastructureMapping).isNotNull();

    ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.ARTIFACTORY_ECHO_WAR);
    assertThat(artifactStream).isNotNull();

    WorkflowPhase phase1 =
        aWorkflowPhase().serviceId(service.getUuid()).infraMappingId(infrastructureMapping.getUuid()).build();

    logger.info("Creating workflow with canary orchestration : " + dummyWorkflow);
    Workflow workflow =
        aWorkflow()
            .name(dummyWorkflow)
            .description(dummyWorkflow)
            .serviceId(service.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .infraMappingId(infrastructureMapping.getUuid())
            .envId(environment.getUuid())
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().withWorkflowPhases(ImmutableList.of(phase1)).build())
            .build();

    savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, AccountGenerator.ACCOUNT_ID, application.getUuid(), workflow);

    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);

    artifact = ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, application.getUuid(), artifactStream.getUuid());

    logger.info("Modifying Workflow Phase to add HTTP command in Verify Step of Phase 1");

    wfUtils.modifyPhasesForPipeline(bearerToken, savedWorkflow, application.getUuid());
  }

  @Test
  @Owner(developers = SUNIL, intermittent = true)
  @Category(FunctionalTests.class)
  public void pipelineTest() throws Exception {
    pipelineName = "Pipeline Test - " + System.currentTimeMillis();
    logger.info("Generated unique pipeline name : " + pipelineName);
    Pipeline pipeline = new Pipeline();
    pipeline.setName(pipelineName);
    pipeline.setDescription("description");
    logger.info("Creating the pipeline");
    Pipeline createdPipeline =
        PipelineRestUtils.createPipeline(application.getAppId(), pipeline, getAccount().getUuid(), bearerToken);
    assertThat(createdPipeline).isNotNull();
    logger.info("Making a get call and verifying if the pipeline created is accessible");
    Pipeline verifyCreatedPipeline =
        PipelineRestUtils.getPipeline(application.getAppId(), createdPipeline.getUuid(), bearerToken);
    assertThat(verifyCreatedPipeline).isNotNull();
    assertThat(createdPipeline.getName().equals(verifyCreatedPipeline.getName())).isTrue();
    logger.info("Create and Get pipeline verification completed");

    logger.info("Creating pipeline stages now");
    List<PipelineStage> pipelineStages = new ArrayList<>();
    PipelineStage executionStage = PipelineUtils.prepareExecutionStage(
        infrastructureMapping.getEnvId(), savedWorkflow.getUuid(), Collections.emptyMap());
    pipelineStages.add(executionStage);
    verifyCreatedPipeline.setPipelineStages(pipelineStages);

    createdPipeline = PipelineRestUtils.updatePipeline(application.getAppId(), verifyCreatedPipeline, bearerToken);
    assertThat(createdPipeline).isNotNull();
    verifyCreatedPipeline =
        PipelineRestUtils.getPipeline(application.getAppId(), createdPipeline.getUuid(), bearerToken);
    assertThat(verifyCreatedPipeline).isNotNull();
    assertThat(verifyCreatedPipeline.getPipelineStages().size() == pipelineStages.size()).isTrue();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);
    List<Artifact> artifacts = new ArrayList<>();
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setExcludeHostsWithSameArtifact(false);
    executionArgs.setExecutionCredential(null);
    executionArgs.setNotifyTriggeredUserOnly(false);
    executionArgs.setPipelineId(verifyCreatedPipeline.getUuid());

    Map<String, Object> pipelineExecution = ExecutionRestUtils.runPipeline(
        bearerToken, application.getAppId(), environment.getUuid(), verifyCreatedPipeline.getUuid(), executionArgs);

    assertThat(pipelineExecution).isNotNull();

    verifyTheExecutionValues(pipelineExecution, verifyCreatedPipeline);
    logger.info("Validation completed");
  }

  private void verifyTheExecutionValues(Map<String, Object> pipelineExecution, Pipeline pipeline) {
    assertThat(pipelineExecution.size() > 0).isTrue();
    assertThat(pipelineExecution.containsKey("appId")).isTrue();
    assertThat(pipelineExecution.containsKey("appName")).isTrue();
    assertThat(pipelineExecution.containsKey("name")).isTrue();
    assertThat(pipelineExecution.containsKey("uuid")).isTrue();

    String actualAppId = pipelineExecution.get("appId").toString();

    String actualAppName = pipelineExecution.get("appName").toString();

    String actualName = pipelineExecution.get("name").toString();

    String pipelineExecutionId = pipelineExecution.get("uuid").toString();

    assertThat(actualAppId.equals(application.getUuid())).isTrue();
    assertThat(actualAppName.equals(application.getName())).isTrue();
    assertThat(actualName.equals(pipeline.getName())).isTrue();

    logger.info("Waiting for 2 mins until the execution is complete");

    Awaitility.await()
        .atMost(240, TimeUnit.SECONDS)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> Setup.portal()
                          .auth()
                          .oauth2(bearerToken)
                          .queryParam("appId", application.getUuid())
                          .get("/executions/" + pipelineExecutionId)
                          .jsonPath()
                          .<String>getJsonObject("resource.status")
                          .equals(ExecutionStatus.SUCCESS.name()));

    logger.info("Workflow execution completed");
    String status =
        ExecutionRestUtils.getExecutionStatus(bearerToken, getAccount(), application.getAppId(), pipelineExecutionId);
    assertThat(status).isEqualTo("SUCCESS");

    WorkflowExecution completedExecution =
        workflowExecutionService.getExecutionDetails(application.getUuid(), pipelineExecutionId, true, null);

    logger.info("Validation starts");

    assertThat(completedExecution.getPipelineExecution().getStatus().name().equals("SUCCESS")).isTrue();
    assertThat(completedExecution.getName().equals(pipelineName)).isTrue();
    assertThat(completedExecution.getPipelineExecution().getPipelineStageExecutions().size() == 1).isTrue();
    assertThat(
        completedExecution.getPipelineExecution().getPipelineStageExecutions().get(0).getWorkflowExecutions().size()
        == 1)
        .isTrue();
    assertThat(completedExecution.getPipelineExecution()
                   .getPipelineStageExecutions()
                   .get(0)
                   .getWorkflowExecutions()
                   .get(0)
                   .getName()
                   .equals(dummyWorkflow))
        .isTrue();
    assertThat(completedExecution.getPipelineExecution()
                   .getPipelineStageExecutions()
                   .get(0)
                   .getWorkflowExecutions()
                   .get(0)
                   .getStatus()
                   .name()
                   .equals("SUCCESS"))
        .isTrue();
    assertThat(completedExecution.getPipelineExecution().getPipelineStageExecutions().size() == 1).isTrue();
  }
}
