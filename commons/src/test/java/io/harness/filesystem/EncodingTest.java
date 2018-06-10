package io.harness.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class EncodingTest {
  @Test
  public void testDefaultEncoding() throws UnsupportedEncodingException {
    assertThat(Charset.defaultCharset()).isEqualTo(Charset.forName("UTF-8"));
  }
}
