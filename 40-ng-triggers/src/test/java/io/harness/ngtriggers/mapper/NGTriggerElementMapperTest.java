package io.harness.ngtriggers.mapper;

import com.google.common.io.Resources;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;

public class NGTriggerElementMapperTest extends CategoryTest {
  private String ngTriggerYaml;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "ng-trigger.yaml";
    ngTriggerYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToTriggerConfig() {
    NGTriggerConfig trigger = NGTriggerElementMapper.toTriggerConfig(ngTriggerYaml);
    assertThat(trigger).isNotNull();
  }
}