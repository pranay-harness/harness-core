package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.schema.beans.FieldSubtypeData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class JacksonSubtypeHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSubtypeMapping() {
    JacksonSubtypeHelper jacksonSubtypeHelper = new JacksonSubtypeHelper();
    Map<String, Set<FieldSubtypeData>> stringModelSet = new HashMap<>();
    jacksonSubtypeHelper.getSubtypeMapping(TestClass.ClassWhichContainsInterface.class, stringModelSet);
    assertThat(stringModelSet).isNotEmpty();
    assertThat(stringModelSet.size()).isEqualTo(4);
    assertThat(stringModelSet.get("ClassWhichContainsInterface").size()).isEqualTo(1);
  }
}