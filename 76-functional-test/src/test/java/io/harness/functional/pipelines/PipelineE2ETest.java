package io.harness.functional.pipelines;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.RestUtils.ArtifactRestUtil;
import io.harness.RestUtils.ArtifactStreamRestUtil;
import io.harness.RestUtils.ExecutionRestUtil;
import io.harness.RestUtils.PipelineRestUtils;
import io.harness.RestUtils.WorkflowRestUtil;
import io.harness.Utils.PipelineUtils;
import io.harness.Utils.WorkflowUtils;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.framework.Retry;
import io.harness.framework.Setup;
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
  @Inject private WorkflowRestUtil workflowRestUtil;
  @Inject private ArtifactRestUtil artifactRestUtil;
  PipelineRestUtils pipelineRestUtils = new PipelineRestUtils();
  ArtifactStreamRestUtil artifactStreamRestUtil = new ArtifactStreamRestUtil();
  ExecutionRestUtil executionRestUtil = new ExecutionRestUtil();

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

  String artifactStreamId = null;

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
    resetCache();
    savedWorkflow = workflowRestUtil.createWorkflow(AccountGenerator.ACCOUNT_ID, application.getUuid(), workflow);

    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);

    //

    artifact = artifactRestUtil.waitAndFetchArtifactByArtfactStream(application.getUuid(), artifactStream.getUuid());

    //    ExecutionArgs executionArgs = new ExecutionArgs();
    //    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());
    //    executionArgs.setExecutionCredential(
    //        SSHExecutionCredential.Builder.aSSHExecutionCredential().executionType(ExecutionType.SSH).build());
    //    executionArgs.setOrchestrationId(savedWorkflow.getUuid());
    //    executionArgs.setArtifacts(Collections.singletonList(artifact));
    //    executionArgs.setServiceId(service.getUuid());

    logger.info("Modifying Workflow Phase to add HTTP command in Verify Step of Phase 1");

    wfUtils.modifyPhasesForPipeline(savedWorkflow, application.getUuid());
  }

  @Test
  @Owner(emails = "swamy@harness.io")
  @Category(FunctionalTests.class)
  public void pipelineTest() throws Exception {
    pipelineName = "Pipeline Test - " + System.currentTimeMillis();
    logger.info("Generated unique pipeline name : " + pipelineName);
    Pipeline pipeline = new Pipeline();
    pipeline.setName(pipelineName);
    pipeline.setDescription("description");
    logger.info("Creating the pipeline");
    Pipeline createdPipeline =
        pipelineRestUtils.createPipeline(application.getAppId(), pipeline, getAccount().getUuid(), bearerToken);
    assertNotNull(createdPipeline);
    logger.info("Making a get call and verifying if the pipeline created is accessible");
    Pipeline verifyCreatedPipeline =
        pipelineRestUtils.getPipeline(application.getAppId(), createdPipeline.getUuid(), bearerToken);
    assertNotNull(verifyCreatedPipeline);
    assertTrue(createdPipeline.getName().equals(verifyCreatedPipeline.getName()));
    logger.info("Create and Get pipeline verification completed");

    logger.info("Creating pipeline stages now");
    List<PipelineStage> pipelineStages = new ArrayList<>();
    PipelineStage executionStage = PipelineUtils.prepareExecutionStage(
        infrastructureMapping.getEnvId(), savedWorkflow.getUuid(), verifyCreatedPipeline.getUuid());
    pipelineStages.add(executionStage);
    verifyCreatedPipeline.setPipelineStages(pipelineStages);

    createdPipeline = pipelineRestUtils.updatePipeline(application.getAppId(), verifyCreatedPipeline, bearerToken);
    assertNotNull(createdPipeline);
    verifyCreatedPipeline =
        pipelineRestUtils.getPipeline(application.getAppId(), createdPipeline.getUuid(), bearerToken);
    assertNotNull(verifyCreatedPipeline);
    assertTrue(verifyCreatedPipeline.getPipelineStages().size() == pipelineStages.size());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);
    List<Artifact> artifacts = new ArrayList<>();
    artifacts.add(artifact);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setExcludeHostsWithSameArtifact(false);
    executionArgs.setExecutionCredential(null);
    executionArgs.setNotifyTriggeredUserOnly(false);
    executionArgs.setPipelineId(verifyCreatedPipeline.getUuid());

    Map<String, Object> pipelineExecution = executionRestUtil.runPipeline(
        application.getAppId(), environment.getUuid(), verifyCreatedPipeline.getUuid(), executionArgs);

    assertThat(pipelineExecution).isNotNull();

    verifyTheExecutionValues(pipelineExecution, verifyCreatedPipeline);
    logger.info("Validation completed");
  }

  private void verifyTheExecutionValues(Map<String, Object> pipelineExecution, Pipeline pipeline) {
    assertTrue(pipelineExecution.size() > 0);
    assertTrue(pipelineExecution.containsKey("appId"));
    assertTrue(pipelineExecution.containsKey("appName"));
    assertTrue(pipelineExecution.containsKey("name"));
    assertTrue(pipelineExecution.containsKey("uuid"));

    String actualAppId = pipelineExecution.get("appId").toString();

    String actualAppName = pipelineExecution.get("appName").toString();

    String actualName = pipelineExecution.get("name").toString();

    String pipelineExecutionId = pipelineExecution.get("uuid").toString();

    assertTrue(actualAppId.equals(application.getUuid()));
    assertTrue(actualAppName.equals(application.getName()));
    assertTrue(actualName.equals(pipeline.getName()));

    logger.info("Waiting for 2 mins until the execution is complete");

    Awaitility.await()
        .atMost(120, TimeUnit.SECONDS)
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
    String status = executionRestUtil.getExecutionStatus(application.getAppId(), pipelineExecutionId);
    assertThat(status).isEqualTo("SUCCESS");

    WorkflowExecution completedExecution =
        workflowExecutionService.getExecutionDetails(application.getUuid(), pipelineExecutionId, true, null);

    logger.info("Validation starts");

    assertTrue(completedExecution.getPipelineExecution().getStatus().name().equals("SUCCESS"));
    assertTrue(completedExecution.getName().equals(pipelineName));
    assertTrue(completedExecution.getPipelineExecution().getPipelineStageExecutions().size() == 1);
    assertTrue(
        completedExecution.getPipelineExecution().getPipelineStageExecutions().get(0).getWorkflowExecutions().size()
        == 1);
    assertTrue(completedExecution.getPipelineExecution()
                   .getPipelineStageExecutions()
                   .get(0)
                   .getWorkflowExecutions()
                   .get(0)
                   .getName()
                   .equals(dummyWorkflow));
    assertTrue(completedExecution.getPipelineExecution()
                   .getPipelineStageExecutions()
                   .get(0)
                   .getWorkflowExecutions()
                   .get(0)
                   .getStatus()
                   .name()
                   .equals("SUCCESS"));
    assertTrue(completedExecution.getPipelineExecution().getPipelineStageExecutions().size() == 1);
  }
}
