package io.harness.execution;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.plan.NodeType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class IdentityNodeExecutionMetadataTest extends CategoryTest {
  IdentityNodeExecutionMetadata identityNodeExecutionMetadata = new IdentityNodeExecutionMetadata();

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetNodeType() {
    assertThat(identityNodeExecutionMetadata.forNodeType()).isEqualTo(NodeType.IDENTITY_PLAN_NODE);
  }
}
