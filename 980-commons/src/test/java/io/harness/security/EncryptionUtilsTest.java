package io.harness.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EncryptionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.VIKAS)
  @Category(UnitTests.class)
  @Ignore("Please fix this")
  public void toBytes() {
    final char[] src = "input-array".toCharArray();
    final byte[] bytes = EncryptionUtils.toBytes(src, Charsets.UTF_8);
    assertThat(String.valueOf(src)).isEqualTo("input-array");
  }
}
