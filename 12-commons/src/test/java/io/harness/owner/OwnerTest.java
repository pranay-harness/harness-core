package io.harness.owner;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class OwnerTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @Ignore("This is dummy failing on purpose tests to test the owner infrastructure")
  public void reportOwner() {
    assertThat(true).isFalse();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("This is dummy failing on purpose tests to test the owner infrastructure")
  public void reportOwner2() {
    assertThat(true).isFalse();
  }
}
