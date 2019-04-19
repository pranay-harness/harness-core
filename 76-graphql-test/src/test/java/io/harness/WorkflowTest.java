package io.harness;

import static io.harness.generator.WorkflowGenerator.Workflows.BASIC_SIMPLE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.WorkflowGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Workflow;
import software.wings.graphql.schema.type.QLWorkflow;

@Slf4j
public class WorkflowTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject WorkflowGenerator workflowGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryWorkflow() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Workflow workflow = workflowGenerator.ensurePredefined(seed, owners, BASIC_SIMPLE);

    String query = "{ workflow(workflowId: \"" + workflow.getUuid() + "\") { id name description } }";

    QLWorkflow qlWorkflow = execute(QLWorkflow.class, query);
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
}
