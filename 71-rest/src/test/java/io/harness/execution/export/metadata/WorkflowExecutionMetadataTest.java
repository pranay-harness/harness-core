package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.WorkflowExecution;

import java.time.Instant;
import java.util.List;

public class WorkflowExecutionMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAccept() {
    MetadataTestHelper.SimpleVisitor simpleVisitor = new MetadataTestHelper.SimpleVisitor();
    WorkflowExecutionMetadata.builder()
        .executionGraph(
            asList(GraphNodeMetadata.builder().id("id1").build(), GraphNodeMetadata.builder().id("id2").build()))
        .build()
        .accept(simpleVisitor);
    assertThat(simpleVisitor.getVisited()).containsExactly("id1", "id2");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromWorkflowExecutions() {
    assertThat(WorkflowExecutionMetadata.fromWorkflowExecutions(null, true)).isNull();

    Instant now = Instant.now();
    List<WorkflowExecutionMetadata> workflowExecutionMetadataList = WorkflowExecutionMetadata.fromWorkflowExecutions(
        asList(null, MetadataTestHelper.prepareWorkflowExecution(now)), true);
    assertThat(workflowExecutionMetadataList).isNotNull();
    assertThat(workflowExecutionMetadataList.size()).isEqualTo(1);
    MetadataTestHelper.validateWorkflowExecutionMetadata(workflowExecutionMetadataList.get(0), now);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromWorkflowExecution() {
    assertThat(WorkflowExecutionMetadata.fromWorkflowExecution(null, true)).isNull();
    assertThat(WorkflowExecutionMetadata.fromWorkflowExecution(
                   WorkflowExecution.builder().workflowType(WorkflowType.PIPELINE).build(), true))
        .isNull();

    Instant now = Instant.now();
    WorkflowExecutionMetadata workflowExecutionMetadata =
        WorkflowExecutionMetadata.fromWorkflowExecution(MetadataTestHelper.prepareWorkflowExecution(now), true);
    MetadataTestHelper.validateWorkflowExecutionMetadata(workflowExecutionMetadata, now);
  }
}
