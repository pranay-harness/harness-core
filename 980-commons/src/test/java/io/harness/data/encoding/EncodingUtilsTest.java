/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.encoding;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.BRETT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.net.URL;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EncodingUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testCompression() throws Exception {
    StringBuilder stringToCompress = new StringBuilder(generateUuid());
    for (int i = 0; i < 100; i++) {
      stringToCompress.append(' ').append(generateUuid());
    }

    byte[] compressedString = compressString(stringToCompress.toString());
    String deCompressedString = deCompressString(compressedString);
    assertThat(deCompressedString).isEqualTo(stringToCompress.toString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testCompressionWithStringEncoding() throws Exception {
    StringBuilder stringToCompress = new StringBuilder(generateUuid());
    for (int i = 0; i < 100; i++) {
      stringToCompress.append(' ').append(generateUuid());
    }

    String compressedString = encodeBase64(compressString(stringToCompress.toString()));
    String deCompressedString = deCompressString(decodeBase64(compressedString));
    assertThat(deCompressedString).isEqualTo(stringToCompress.toString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testBase64() throws Exception {
    URL url = this.getClass().getResource("/dos-config.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    assertThat(fileContents).isEqualTo(decodeBase64ToString(encodeBase64(fileContents)));
  }
}
