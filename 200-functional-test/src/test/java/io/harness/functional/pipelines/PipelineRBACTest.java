package io.harness.functional.pipelines;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.utils.K8SUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.PipelineUtils;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.infra.InfrastructureDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class PipelineRBACTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private ArtifactStreamManager artifactStreamManager;

  private Application application;
  private Environment qaEnvironment;
  private Environment prodEnvironment;
  private InfrastructureDefinition qaInfrastructureDefinition;
  private InfrastructureDefinition prodInfrastructureDefinition;
  private Workflow savedWorkflow;

  final Seed seed = new Seed(0);
  Owners owners;

  @Before
  public void createAllEntities() {
    owners = ownerManager.create();

    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    Service service = serviceGenerator.ensurePredefined(seed, owners, Services.K8S_V2_TEST);
    assertThat(service).isNotNull();

    qaEnvironment = environmentGenerator.ensurePredefined(seed, owners, Environments.PIPELINE_RBAC_QA_TEST);
    assertThat(qaEnvironment).isNotNull();

    prodEnvironment = environmentGenerator.ensurePredefined(seed, owners, Environments.PIPELINE_RBAC_PROD_TEST);
    assertThat(prodEnvironment).isNotNull();

    qaInfrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureDefinitions.PIPELINE_RBAC_QA_AWS_SSH_TEST);
    assertThat(qaInfrastructureDefinition).isNotNull();

    prodInfrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureDefinitions.PIPELINE_RBAC_PROD_AWS_SSH_TEST);
    assertThat(prodInfrastructureDefinition).isNotNull();

    ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_DOCKER);
    assertThat(artifactStream).isNotNull();

    logger.info("Creating k8s rolling workflow");
    savedWorkflow = K8SUtils.createWorkflow(application.getUuid(), qaEnvironment.getUuid(), service.getUuid(),
        qaInfrastructureDefinition.getUuid(), "Workflow - Pipeline RBAC - " + System.currentTimeMillis(),
        OrchestrationWorkflowType.ROLLING, bearerToken, application.getAccountId());
    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);

    logger.info("Setup complete");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(FunctionalTests.class)
  public void testPipelineRBAC() {
    String pipelineName = "Pipeline RBAC Test - " + System.currentTimeMillis();
    logger.info("Pipeline name: " + pipelineName);

    Pipeline pipeline = new Pipeline();
    pipeline.setName(pipelineName);
    pipeline.setDescription("description");

    logger.info("Creating pipeline...");
    Pipeline createdPipeline =
        PipelineRestUtils.createPipeline(application.getAppId(), pipeline, getAccount().getUuid(), bearerToken);
    assertThat(createdPipeline).isNotNull();

    logger.info("Verifying pipeline...");
    Pipeline verifyCreatedPipeline =
        PipelineRestUtils.getPipeline(application.getAppId(), createdPipeline.getUuid(), bearerToken);
    assertThat(verifyCreatedPipeline).isNotNull();
    assertThat(createdPipeline.getName().equals(verifyCreatedPipeline.getName())).isTrue();
    logger.info("Created pipeline");

    logger.info("Creating pipeline stages...");
    List<PipelineStage> pipelineStages = new ArrayList<>();
    PipelineStage executionStage = PipelineUtils.prepareExecutionStage(
        qaInfrastructureDefinition.getEnvId(), savedWorkflow.getUuid(), Collections.emptyMap());
    pipelineStages.add(executionStage);
    verifyCreatedPipeline.setPipelineStages(pipelineStages);

    logger.info("Updating pipeline...");
    createdPipeline = PipelineRestUtils.updatePipeline(application.getAppId(), verifyCreatedPipeline, bearerToken);
    assertThat(createdPipeline).isNotNull();

    logger.info("Verifying updated pipeline...");
    verifyCreatedPipeline =
        PipelineRestUtils.getPipeline(application.getAppId(), createdPipeline.getUuid(), bearerToken);
    assertThat(verifyCreatedPipeline).isNotNull();
    assertThat(verifyCreatedPipeline.getPipelineStages().size() == pipelineStages.size()).isTrue();
    logger.info("Updated pipeline");

    logger.info("Updating workflow environment...");
    savedWorkflow.setEnvId(prodEnvironment.getUuid());
    savedWorkflow.setInfraDefinitionId(prodInfrastructureDefinition.getUuid());
    savedWorkflow =
        WorkflowRestUtils.updateWorkflow(bearerToken, application.getAccountId(), application.getUuid(), savedWorkflow);
    logger.info("Updated workflow environment");

    logger.info("Deleting environment...");
    EnvironmentRestUtils.deleteEnvironment(
        bearerToken, application.getUuid(), application.getAccountId(), qaEnvironment.getUuid());
    logger.info("Deleted environment");

    logger.info("Verifying if pipeline still accessible...");
    verifyCreatedPipeline =
        PipelineRestUtils.getPipeline(application.getAppId(), createdPipeline.getUuid(), bearerToken);
    assertThat(verifyCreatedPipeline).isNotNull();
    assertThat(verifyCreatedPipeline.getName()).isEqualTo(pipelineName);
    logger.info("Validation completed");
  }
}
