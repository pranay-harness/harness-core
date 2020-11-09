package io.harness.data.structure;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MongoMapSanitizerTest extends CategoryTest {
  private final MongoMapSanitizer mongoMapSanitizer = new MongoMapSanitizer('~');

  @Test
  @Owner(developers = OwnerRule.AVMOHAN)
  @Category(UnitTests.class)
  public void testEncode() throws Exception {
    ImmutableMap<String, String> pre = of("key1", "val1", "key.23", "val2", "key3", "val.23");
    ImmutableMap<String, String> post = of("key1", "val1", "key~23", "val2", "key3", "val.23");
    assertThat(mongoMapSanitizer.encodeDotsInKey(pre)).isEqualTo(post);
  }

  @Test
  @Owner(developers = OwnerRule.AVMOHAN)
  @Category(UnitTests.class)
  public void testDecode() throws Exception {
    ImmutableMap<String, String> pre = of("key1", "val1", "key~23", "val2", "key3", "val.23");
    ImmutableMap<String, String> post = of("key1", "val1", "key.23", "val2", "key3", "val.23");
    assertThat(mongoMapSanitizer.decodeDotsInKey(pre)).isEqualTo(post);
  }
}
