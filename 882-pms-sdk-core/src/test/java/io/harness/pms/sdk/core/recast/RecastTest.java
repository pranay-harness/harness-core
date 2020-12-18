package io.harness.pms.sdk.core.recast;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RecastTest extends PmsSdkCoreTestBase {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestInitialize() {
    Recast recast = new Recast(ImmutableSet.of(OnFailAdviserParameters.class));
    assertThat(recast).isNotNull();
  }
}