package io.harness.data.structure;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

public class MapUtilTest {
  @Test
  @Category(UnitTests.class)
  public void shouldPutIfNotEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtils.putIfNotEmpty("key", "value", input);
    assertNotNull(input.get("key"));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotPutIfEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtils.putIfNotEmpty("key", "", input);
    assertNull(input.get("key"));
  }
}