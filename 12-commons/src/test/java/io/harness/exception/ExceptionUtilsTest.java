package io.harness.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ExceptionUtilsTest {
  @Test
  public void testCause() {
    assertThat(ExceptionUtils.cause(Exception.class, null)).isNull();
    assertThat(ExceptionUtils.cause(Exception.class, new Exception())).isNotNull();

    assertThat(ExceptionUtils.cause(RuntimeException.class, new Exception())).isNull();
    assertThat(ExceptionUtils.cause(RuntimeException.class, new Exception(new RuntimeException()))).isNotNull();
  }
}