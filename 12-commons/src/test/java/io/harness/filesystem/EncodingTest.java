package io.harness.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;

import org.junit.Test;

import java.nio.charset.Charset;

public class EncodingTest {
  @Test
  public void testDefaultEncoding() {
    assertThat(Charset.defaultCharset()).isEqualTo(Charsets.UTF_8);
  }
}
