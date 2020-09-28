package io.harness.mongo;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.LogKeyUtils;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LogKeyUtilsTest extends PersistenceTest {
  static class DummySampleEntity extends SampleEntity {}

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCalculateEntityIdName() {
    assertThat(LogKeyUtils.calculateLogKeyForId(SampleEntity.class)).isEqualTo("sampleEntityId");
    assertThat(LogKeyUtils.calculateLogKeyForId(DummySampleEntity.class)).isEqualTo("sampleEntityId");

    assertThat(LogKeyUtils.calculateLogKeyForId(Object.class)).isEqualTo("");
  }
}