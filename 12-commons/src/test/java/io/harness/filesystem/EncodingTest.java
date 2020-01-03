package io.harness.filesystem;

import static io.harness.rule.OwnerRule.BRETT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.charset.Charset;

public class EncodingTest extends CategoryTest {
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testDefaultEncoding() {
    assertThat(Charset.defaultCharset()).isEqualTo(Charsets.UTF_8);
  }
}
