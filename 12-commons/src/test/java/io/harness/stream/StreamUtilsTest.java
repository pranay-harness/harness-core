package io.harness.stream;

import static io.harness.rule.OwnerRule.AADITI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

@Slf4j
public class StreamUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testGetInputStreamSize() throws Exception {
    String fileContent = "test";
    InputStream is = new ByteArrayInputStream(fileContent.getBytes(Charset.defaultCharset()));
    long size = StreamUtils.getInputStreamSize(is);
    assertThat(size).isEqualTo(4L);
  }
}
