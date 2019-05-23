package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.InfrastructureMappingGenerator.InfrastructureMappings.AWS_SSH_TEST;
import static io.harness.generator.WorkflowGenerator.Workflows.BASIC_SIMPLE;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.InfrastructureMappingGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.WorkflowGenerator;
import io.harness.testframework.graphql.QLTestObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowKeys;
import software.wings.graphql.schema.type.QLWorkflowConnection;

@Slf4j
public class WorkflowTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject WorkflowGenerator workflowGenerator;
  @Inject InfrastructureMappingGenerator infrastructureMappingGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryWorkflow() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Workflow workflow = workflowGenerator.ensurePredefined(seed, owners, BASIC_SIMPLE);

    String query = $GQL(/*
{
  workflow(workflowId: "%s") {
    id
    name
    description
    createdAt
    createdBy {
      id
    }
  }
}*/ workflow.getUuid());

    QLTestObject qlWorkflow = qlExecute(query);
    assertThat(qlWorkflow.get(QLWorkflowKeys.id)).isEqualTo(workflow.getUuid());
    assertThat(qlWorkflow.get(QLWorkflowKeys.name)).isEqualTo(workflow.getName());
    assertThat(qlWorkflow.get(QLWorkflowKeys.description)).isEqualTo(workflow.getDescription());
    assertThat(qlWorkflow.get(QLWorkflowKeys.createdAt))
        .isEqualTo(GraphQLDateTimeScalar.convertToString(workflow.getCreatedAt()));
    assertThat(qlWorkflow.sub(QLWorkflowKeys.createdBy).get(QLUserKeys.id))
        .isEqualTo(workflow.getCreatedBy().getUuid());
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryMissingPipeline() {
    String query = $GQL(/*
{
  workflow(workflowId: "blah") {
    id
  }
}*/);

    final ExecutionResult result = qlResult(query);
    assertThat(result.getErrors().size()).isEqualTo(1);
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryWorkflows() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().name("Application Workflows").build());
    owners.add(application);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    final WorkflowBuilder builder =
        aWorkflow()
            .infraMappingId(infrastructureMapping.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                       .build());

    final Workflow workflow1 = workflowGenerator.ensureWorkflow(seed, owners, builder.name("workflow1").build());
    final Workflow workflow2 = workflowGenerator.ensureWorkflow(seed, owners, builder.name("workflow2").build());
    final Workflow workflow3 = workflowGenerator.ensureWorkflow(seed, owners, builder.name("workflow3").build());

    {
      String query = $GQL(/*
{
  workflows(applicationId: "%s" limit: 2) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLWorkflowConnection workflowConnection = qlExecute(QLWorkflowConnection.class, query);
      assertThat(workflowConnection.getNodes().size()).isEqualTo(2);

      assertThat(workflowConnection.getNodes().get(0).getId()).isEqualTo(workflow3.getUuid());
      assertThat(workflowConnection.getNodes().get(1).getId()).isEqualTo(workflow2.getUuid());
    }
    {
      String query = $GQL(/*
{
  workflows(applicationId: "%s" limit: 2 offset: 1) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLWorkflowConnection workflowConnection = qlExecute(QLWorkflowConnection.class, query);
      assertThat(workflowConnection.getNodes().size()).isEqualTo(2);

      assertThat(workflowConnection.getNodes().get(0).getId()).isEqualTo(workflow2.getUuid());
      assertThat(workflowConnection.getNodes().get(1).getId()).isEqualTo(workflow1.getUuid());
    }

    {
      String query = $GQL(/*
{
application(applicationId: "%s") {
  workflows(limit: 5) {
    nodes {
      id
    }
  }
}
}*/ application.getUuid());

      final QLTestObject qlTestObject = qlExecute(query);
      assertThat(qlTestObject.getMap().size()).isEqualTo(1);
    }
  }
}
