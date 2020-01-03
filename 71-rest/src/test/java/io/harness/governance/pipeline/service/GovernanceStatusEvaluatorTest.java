package io.harness.governance.pipeline.service;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.governance.pipeline.service.model.Tag;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.HarnessTagLink;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GovernanceStatusEvaluatorTest extends WingsBaseTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testContainsAll() {
    List<HarnessTagLink> links = Arrays.asList(tagLink("color", "blue"), tagLink("onprem", null));
    List<Tag> tags = Collections.singletonList(new Tag("color", null));

    assertThat(GovernanceStatusEvaluator.containsAll(links, tags)).isTrue();

    tags = Collections.singletonList(new Tag("color", "red"));
    assertThat(GovernanceStatusEvaluator.containsAll(links, tags)).isFalse();

    tags = Arrays.asList(new Tag("color", null), new Tag("onprem", "yes"));
    assertThat(GovernanceStatusEvaluator.containsAll(links, tags)).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testContainsAny() {
    List<HarnessTagLink> links = Arrays.asList(tagLink("color", "blue"), tagLink("onprem", null));
    List<Tag> tags = Arrays.asList(new Tag("color", null), new Tag("onprem", "yes"));

    assertThat(GovernanceStatusEvaluator.containsAny(links, tags)).isTrue();
  }

  private HarnessTagLink tagLink(String key, String value) {
    return HarnessTagLink.builder().key(key).value(value).build();
  }
}
