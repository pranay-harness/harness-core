package io.harness.functional.graphql;

import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateType.ENV_STATE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.WorkflowGenerator;
import io.harness.testframework.graphql.QLTestObject;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.schema.type.QLExecutionConnection.QLExecutionConnectionKeys;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoKeys;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionKeys;

import java.util.ArrayList;

public class GraphQLExecutionTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private PipelineGenerator pipelineGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;

  public Workflow buildWorkflow(Seed seed, Owners owners) {
    // Test  creating a workflow
    Workflow workflow = workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .name("Workflow - " + generateUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                       .build())
            .build());

    resetCache();
    return workflow;
  }

  @NotNull
  public WorkflowExecution executeWorkflow(Workflow workflow, Application application, Environment environment) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(workflow.getUuid());

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();
    return workflowExecution;
  }

  @Test
  @Category({FunctionalTests.class, GraphQLTests.class})
  public void fetchExecutionsInRange() throws Exception {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();
    Workflow workflow = buildWorkflow(seed, owners);

    final Application application = owners.obtainApplication();
    final Environment environment = owners.obtainEnvironment();

    // Test running the workflow

    WorkflowExecution workflowExecution1 = executeWorkflow(workflow, application, environment);
    WorkflowExecution workflowExecution2 = executeWorkflow(workflow, application, environment);

    {
      String query = format("{ executions(workflowId: \"%s\", limit: 5, from: %s, to: %s) "
              + "{ pageInfo { total } nodes { id } } }",
          workflow.getUuid(), workflowExecution1.getCreatedAt(), workflowExecution2.getCreatedAt());

      final QLTestObject qlTestObject = qlExecute(query);

      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(1);
    }
  }

  @Test
  @Category({FunctionalTests.class, GraphQLTests.class})
  public void fetchWorkflowExecution() throws Exception {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();
    Workflow workflow = buildWorkflow(seed, owners);

    final Application application = owners.obtainApplication();
    final Environment environment = owners.obtainEnvironment();

    // Test running the workflow

    WorkflowExecution workflowExecution = executeWorkflow(workflow, application, environment);

    {
      String query = "{ execution(executionId: \"" + workflowExecution.getUuid()
          + "\") { id triggeredAt startedAt endedAt status } }";

      final QLTestObject qlTestObject = qlExecute(query);

      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.id)).isEqualTo(workflowExecution.getUuid());
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.triggeredAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.startedAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.endedAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.status)).isEqualTo("SUCCESS");
    }

    {
      String query =
          "{ executions(workflowId: \"" + workflow.getUuid() + "\", limit: 5) { pageInfo { total } nodes { id } } }";

      final QLTestObject qlTestObject = qlExecute(query);
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(1);
    }

    executeWorkflow(workflow, application, environment);

    {
      String query =
          "{ executions(workflowId: \"" + workflow.getUuid() + "\", limit: 5) { pageInfo { total } nodes { id } } }";

      final QLTestObject qlTestObject = qlExecute(query);
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(2);
    }

    {
      String query =
          "{ workflow(workflowId: \"" + workflow.getUuid() + "\") { executions(limit: 2) { nodes { id } } } }";

      final QLTestObject qlTestObject = qlExecute(query);

      assertThat(((ArrayList) qlTestObject.sub("executions").get("nodes")).size()).isEqualTo(2);
    }
  }

  @Test
  @Category({FunctionalTests.class, GraphQLTests.class})
  public void fetchPipelineExecution() throws Exception {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();

    // Test  creating a workflow
    Workflow savedWorkflow = workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .name("Workflow - " + generateUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                       .build())
            .build());

    final Pipeline pipeline = pipelineGenerator.ensurePipeline(seed, owners,
        Pipeline.builder()
            .name("Pipeline - " + generateUuid())
            .pipelineStages(asList(
                PipelineStage.builder()
                    .pipelineStageElements(asList(PipelineStageElement.builder()
                                                      .name("Parallel section 1-1")
                                                      .type(ENV_STATE.name())
                                                      .properties(ImmutableMap.of("envId", savedWorkflow.getEnvId(),
                                                          "workflowId", savedWorkflow.getUuid()))
                                                      .build()))
                    .build()))
            .build());

    resetCache();

    // Test running the workflow

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(PIPELINE);
    executionArgs.setPipelineId(pipeline.getUuid());

    final Application application = owners.obtainApplication();
    final Environment environment = owners.obtainEnvironment();

    WorkflowExecution workflowExecution =
        runPipeline(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();

    {
      String query = "{ execution(executionId: \"" + workflowExecution.getUuid()
          + "\") { id triggeredAt startedAt endedAt status } }";

      final QLTestObject qlTestObject = qlExecute(query);
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.id)).isEqualTo(workflowExecution.getUuid());
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.triggeredAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.startedAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.endedAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.status)).isEqualTo("SUCCESS");
    }

    {
      String query =
          "{ executions(pipelineId: \"" + pipeline.getUuid() + "\", limit: 5) { pageInfo { total } nodes { id } } }";

      final QLTestObject qlTestObject = qlExecute(query);
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(1);
    }

    runPipeline(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);

    {
      String query =
          "{ executions(pipelineId: \"" + pipeline.getUuid() + "\", limit: 5) { pageInfo { total } nodes { id } } }";

      final QLTestObject qlTestObject = qlExecute(query);
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(2);
    }

    {
      String query =
          "{ pipeline(pipelineId: \"" + pipeline.getUuid() + "\") { executions(limit: 2) { nodes { id } } } }";

      final QLTestObject qlTestObject = qlExecute(query);

      assertThat(((ArrayList) qlTestObject.sub("executions").get("nodes")).size()).isEqualTo(2);
    }
  }

  @Test
  @Category({FunctionalTests.class, GraphQLTests.class})
  // TODO: add test for executions by service
  public void fetchServiceExecution() throws Exception {}
}
