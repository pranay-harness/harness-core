package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.WorkflowExecution;

public class TriggeredByMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromWorkflowExecution() {
    assertThat(TriggeredByMetadata.fromWorkflowExecution(null)).isNull();
    assertThat(TriggeredByMetadata.fromWorkflowExecution(WorkflowExecution.builder().build())).isNull();

    TriggeredByMetadata triggeredByMetadata = TriggeredByMetadata.fromWorkflowExecution(
        WorkflowExecution.builder().triggeredBy(EmbeddedUser.builder().name("n").email("e").build()).build());
    assertThat(triggeredByMetadata).isNotNull();
    assertThat(triggeredByMetadata.getType()).isEqualTo(CreatedByType.USER);
    assertThat(triggeredByMetadata.getName()).isEqualTo("n");
    assertThat(triggeredByMetadata.getEmail()).isEqualTo("e");

    triggeredByMetadata =
        TriggeredByMetadata.fromWorkflowExecution(WorkflowExecution.builder()
                                                      .createdByType(CreatedByType.API_KEY)
                                                      .triggeredBy(EmbeddedUser.builder().name("n").email("e").build())
                                                      .build());
    assertThat(triggeredByMetadata).isNotNull();
    assertThat(triggeredByMetadata.getType()).isEqualTo(CreatedByType.API_KEY);
    assertThat(triggeredByMetadata.getName()).isEqualTo("n");
    assertThat(triggeredByMetadata.getEmail()).isNull();

    triggeredByMetadata =
        TriggeredByMetadata.fromWorkflowExecution(WorkflowExecution.builder()
                                                      .createdByType(CreatedByType.TRIGGER)
                                                      .triggeredBy(EmbeddedUser.builder().name("n").email("e").build())
                                                      .build());
    assertThat(triggeredByMetadata).isNotNull();
    assertThat(triggeredByMetadata.getType()).isEqualTo(CreatedByType.TRIGGER);
    assertThat(triggeredByMetadata.getName()).isEqualTo("n");
    assertThat(triggeredByMetadata.getEmail()).isNull();

    triggeredByMetadata =
        TriggeredByMetadata.fromWorkflowExecution(WorkflowExecution.builder()
                                                      .deploymentTriggerId("trigger_id")
                                                      .triggeredBy(EmbeddedUser.builder().name("n").email("e").build())
                                                      .build());
    assertThat(triggeredByMetadata).isNotNull();
    assertThat(triggeredByMetadata.getType()).isEqualTo(CreatedByType.TRIGGER);
    assertThat(triggeredByMetadata.getName()).isEqualTo("n");
    assertThat(triggeredByMetadata.getEmail()).isNull();
  }
}
