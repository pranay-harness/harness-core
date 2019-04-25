package io.harness.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.charset.Charset;

public class EncodingTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testDefaultEncoding() {
    assertThat(Charset.defaultCharset()).isEqualTo(Charsets.UTF_8);
  }
}
