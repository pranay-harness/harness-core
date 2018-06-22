package io.harness.data.structure;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MapUtilTest {
  @Test
  public void shouldPutIfNotEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtil.putIfNotEmpty("key", "value", input);
    assertNotNull(input.get("key"));
  }

  @Test
  public void shouldNotPutIfEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtil.putIfNotEmpty("key", "", input);
    assertNull(input.get("key"));
  }
}