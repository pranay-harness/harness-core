package io.harness.pms.creator;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PipelineCreatorMergeServiceTest extends PipelineServiceTestBase {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void emptyTest() {
    assertThat(true).isTrue();
  }
}
