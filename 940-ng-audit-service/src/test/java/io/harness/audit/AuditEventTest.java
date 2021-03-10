package io.harness.audit;

import static io.harness.rule.OwnerRule.KARAN;

import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AuditEventTest extends CategoryTest {
  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testTrue() {
    // empty test for bazel sync
  }
}
