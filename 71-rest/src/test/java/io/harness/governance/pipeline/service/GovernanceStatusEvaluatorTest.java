package io.harness.governance.pipeline.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.UnitTests;
import io.harness.governance.pipeline.model.Tag;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.HarnessTagLink;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GovernanceStatusEvaluatorTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testContainsAll() {
    List<HarnessTagLink> links = Arrays.asList(tagLink("color", "blue"), tagLink("onprem", null));
    List<Tag> tags = Collections.singletonList(new Tag("color", null));

    assertTrue(GovernanceStatusEvaluator.containsAll(links, tags));

    tags = Collections.singletonList(new Tag("color", "red"));
    assertFalse(GovernanceStatusEvaluator.containsAll(links, tags));

    tags = Arrays.asList(new Tag("color", null), new Tag("onprem", "yes"));
    assertFalse(GovernanceStatusEvaluator.containsAll(links, tags));
  }

  @Test
  @Category(UnitTests.class)
  public void testContainsAny() {
    List<HarnessTagLink> links = Arrays.asList(tagLink("color", "blue"), tagLink("onprem", null));
    List<Tag> tags = Arrays.asList(new Tag("color", null), new Tag("onprem", "yes"));

    assertTrue(GovernanceStatusEvaluator.containsAny(links, tags));
  }

  private HarnessTagLink tagLink(String key, String value) {
    return HarnessTagLink.builder().key(key).value(value).build();
  }
}
