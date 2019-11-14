package io.harness.data.structure;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

public class MapUtilTest extends CategoryTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldPutIfNotEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtils.putIfNotEmpty("key", "value", input);
    assertThat(input.get("key")).isNotNull();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldNotPutIfEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtils.putIfNotEmpty("key", "", input);
    assertThat(input.get("key")).isNull();
  }
}