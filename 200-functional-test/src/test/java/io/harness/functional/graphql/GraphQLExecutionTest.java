/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.graphql;

import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateType.ENV_STATE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.WorkflowGenerator;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

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
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.schema.type.QLApplication.QLApplicationKeys;
import software.wings.graphql.schema.type.QLExecuteOptions;
import software.wings.graphql.schema.type.QLExecutedByUser.QLExecutedByUserKeys;
import software.wings.graphql.schema.type.QLExecutionConnection.QLExecutionConnectionKeys;
import software.wings.graphql.schema.type.QLExecutionStatus;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoKeys;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineKeys;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionKeys;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowKeys;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionKeys;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class GraphQLExecutionTest extends AbstractFunctionalTest {
  public static final String NOTES = "execution test";
  @Inject private HPersistence persistence;
  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private PipelineGenerator pipelineGenerator;

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

    return workflow;
  }

  @NotNull
  public WorkflowExecution executeWorkflow(Workflow workflow, Application application, Environment environment) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setNotes(NOTES);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(workflow.getUuid());

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();
    return workflowExecution;
  }

  @Test
  @Owner(developers = GEORGE, intermittent = true)
  @Category({FunctionalTests.class, GraphQLTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void fetchExecutionsInRange() throws Exception {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();
    Workflow workflow = buildWorkflow(seed, owners);

    final Application application = owners.obtainApplication();
    final Environment environment = owners.obtainEnvironment();

    resetCache(application.getAccountId());
    // Test running the workflow

    WorkflowExecution workflowExecution1 = executeWorkflow(workflow, application, environment);
    WorkflowExecution workflowExecution2 = executeWorkflow(workflow, application, environment);

    {
      String query =
          $GQL(/*
{
executions(filters:[{workflow:{operator:EQUALS,values:["%s"]}},{creationTime:{operator:AFTER,value:%s}},{creationTime:{operator:BEFORE,value:%s}}]
limit: 5) { pageInfo { total
}
nodes {
id
}
}
}*/ workflow.getUuid(), workflowExecution1.getCreatedAt(), workflowExecution2.getCreatedAt());
      final QLTestObject qlTestObject = qlExecute(query, application.getAccountId());
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(2);
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category({FunctionalTests.class, GraphQLTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void fetchWorkflowExecution() throws Exception {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();
    Workflow workflow = buildWorkflow(seed, owners);

    final Application application = owners.obtainApplication();
    final Environment environment = owners.obtainEnvironment();

    resetCache(application.getAccountId());

    // Test running the workflow
    WorkflowExecution workflowExecution = executeWorkflow(workflow, application, environment);
    String accountId = application.getAccountId();
    {
      String query = $GQL(/*
{
  execution(executionId: "%s") {
    id
    application {
      id
    }
    createdAt
    startedAt
    endedAt
    status
    cause {
      __typename
      ... on ExecutedByUser {
        user {
          id
        }
        using
      }
    }
    notes
    ... on WorkflowExecution {
      workflow {
        id
      }
      outcomes {
        nodes {
          ... on DeploymentOutcome {
            service {
              id
            }
            environment {
              id
            }
          }
        }
      }
    }
  }
}*/ workflowExecution.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);

      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.id)).isEqualTo(workflowExecution.getUuid());
      assertThat(qlTestObject.sub("application").get(QLApplicationKeys.id)).isEqualTo(application.getUuid());
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.createdAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.startedAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.endedAt)).isNotNull();
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.status)).isEqualTo(QLExecutionStatus.SUCCESS.name());
      assertThat(qlTestObject.sub(QLWorkflowExecutionKeys.cause).sub(QLExecutedByUserKeys.user).get(QLUserKeys.id))
          .isEqualTo(workflowExecution.getTriggeredBy().getUuid());
      assertThat(qlTestObject.sub(QLWorkflowExecutionKeys.cause).get(QLExecutedByUserKeys.using))
          .isEqualTo(QLExecuteOptions.WEB_UI.name());
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.notes)).isEqualTo(NOTES);
      assertThat(qlTestObject.sub("workflow").get(QLWorkflowKeys.id)).isEqualTo(workflow.getUuid());
    }

    {
      String query = $GQL(/*
{
  executions(filters:[{workflow:{operator:EQUALS,values:["%s"]}}], limit: 5) {
    pageInfo {
      total
    }
    nodes {
      id
    }
  }
}*/ workflow.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(1);
    }

    executeWorkflow(workflow, application, environment);

    {
      String query = $GQL(/*
{
  executions(filters:[{workflow:{operator:EQUALS,values:["%s"]}}], limit: 5) {
    pageInfo {
      total
    }
    nodes {
      id
    }
  }
}*/ workflow.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(2);
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category({FunctionalTests.class, GraphQLTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
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

    // Test running the workflow

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setNotes(NOTES);
    executionArgs.setWorkflowType(PIPELINE);
    executionArgs.setPipelineId(pipeline.getUuid());

    final Application application = owners.obtainApplication();
    final String accountId = application.getAccountId();
    final Environment environment = owners.obtainEnvironment();

    resetCache(accountId);
    WorkflowExecution pipelineExecution =
        runPipeline(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(pipelineExecution).isNotNull();

    {
      String query = $GQL(/*
{
  execution(executionId: "%s") {
    id
    application {
      id
    }
    createdAt
    startedAt
    endedAt
    status
    cause {
      __typename
      ... on ExecutedByUser {
        user {
          id
        }
        using
      }
    }
    notes
    ... on PipelineExecution {
      pipeline {
        id
      }
    }
  }
}*/ pipelineExecution.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);
      assertThat(qlTestObject.get(QLPipelineExecutionKeys.id)).isEqualTo(pipelineExecution.getUuid());
      assertThat(qlTestObject.sub("application").get(QLApplicationKeys.id)).isEqualTo(application.getUuid());
      assertThat(qlTestObject.get(QLPipelineExecutionKeys.createdAt)).isNotNull();
      assertThat(qlTestObject.get(QLPipelineExecutionKeys.startedAt)).isNotNull();
      assertThat(qlTestObject.get(QLPipelineExecutionKeys.endedAt)).isNotNull();
      assertThat(qlTestObject.get(QLPipelineExecutionKeys.status)).isEqualTo(QLExecutionStatus.SUCCESS.name());
      assertThat(qlTestObject.sub(QLPipelineExecutionKeys.cause).sub(QLExecutedByUserKeys.user).get(QLUserKeys.id))
          .isEqualTo(pipelineExecution.getTriggeredBy().getUuid());
      assertThat(qlTestObject.sub(QLPipelineExecutionKeys.cause).get(QLExecutedByUserKeys.using))
          .isEqualTo(QLExecuteOptions.WEB_UI.name());
      assertThat(qlTestObject.get(QLWorkflowExecutionKeys.notes)).isEqualTo(NOTES);
      assertThat(qlTestObject.sub("pipeline").get(QLPipelineKeys.id)).isEqualTo(pipeline.getUuid());
    }

    {
      String query = $GQL(/*
{
  executions(filters:[{pipeline:{operator:EQUALS,values:["%s"]}}], limit: 5) {
    pageInfo {
      total
    }
    nodes {
      id
    }
  }
}*/ pipeline.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(1);
    }

    runPipeline(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);

    {
      String query = $GQL(/*
{
  executions(filters:[{pipeline:{operator:EQUALS,values:["%s"]}}], limit: 5) {
    pageInfo {
      total
    }
    nodes {
      id
    }
  }
}*/ pipeline.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);
      assertThat(qlTestObject.sub(QLExecutionConnectionKeys.pageInfo).get(QLPageInfoKeys.total)).isEqualTo(2);
    }

    {
      final String workflowExecutionId =
          persistence.createQuery(WorkflowExecution.class)
              .filter(WorkflowExecutionKeys.pipelineExecutionId, pipelineExecution.getUuid())
              .get()
              .getUuid();

      String query = $GQL(/*
{
  execution(executionId: "%s") {
    cause {
      __typename
      ... on ExecutedAlongPipeline {
        execution {
          id
        }
      }
    }
  }
}*/ workflowExecutionId);

      final QLTestObject qlTestObject = qlExecute(query, accountId);

      assertThat(qlTestObject.sub(QLPipelineExecutionKeys.cause).sub("execution").get(QLPipelineExecutionKeys.id))
          .isEqualTo(pipelineExecution.getUuid());
    }
  }

  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Category({FunctionalTests.class, GraphQLTests.class})
  // TODO: add test for executions by service
  //  public void fetchServiceExecution() throws Exception {}
}
