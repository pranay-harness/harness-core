package io.harness;

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
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.InfrastructureMappingGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.WorkflowGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.graphql.schema.type.QLWorkflow;
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

    final Workflow workflow = workflowGenerator.ensurePredefined(seed, owners, BASIC_SIMPLE);

    String query = "{ workflow(workflowId: \"" + workflow.getUuid() + "\") { id name description } }";

    QLWorkflow qlWorkflow = qlExecute(QLWorkflow.class, query);
    assertThat(qlWorkflow.getId()).isEqualTo(workflow.getUuid());
    assertThat(qlWorkflow.getName()).isEqualTo(workflow.getName());
    assertThat(qlWorkflow.getDescription()).isEqualTo(workflow.getDescription());
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryMissingPipeline() {
    String query = "{ workflow(workflowId: \"blah\") { id name description } }";

    final ExecutionResult result = getGraphQL().execute(query);
    assertThat(result.getErrors().size()).isEqualTo(1);

    // TODO: this message is wrong
    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo("Exception while fetching data (/workflow) : INVALID_REQUEST");
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryWorkflows() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().withName("Application Workflows").build());
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
      String query =
          "{ workflows(applicationId: \"" + application.getUuid() + "\", limit: 2) { nodes { id name description } } }";

      QLWorkflowConnection workflowConnection = qlExecute(QLWorkflowConnection.class, query);
      assertThat(workflowConnection.getNodes().size()).isEqualTo(2);

      assertThat(workflowConnection.getNodes().get(0).getId()).isEqualTo(workflow3.getUuid());
      assertThat(workflowConnection.getNodes().get(1).getId()).isEqualTo(workflow2.getUuid());
    }
    {
      String query = "{ workflows(applicationId: \"" + application.getUuid()
          + "\", limit: 2, offset: 1) { nodes { id name description } } }";

      QLWorkflowConnection workflowConnection = qlExecute(QLWorkflowConnection.class, query);
      assertThat(workflowConnection.getNodes().size()).isEqualTo(2);

      assertThat(workflowConnection.getNodes().get(0).getId()).isEqualTo(workflow2.getUuid());
      assertThat(workflowConnection.getNodes().get(1).getId()).isEqualTo(workflow1.getUuid());
    }
  }
}
