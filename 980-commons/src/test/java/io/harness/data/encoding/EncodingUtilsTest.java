/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.data.encoding;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ABHINAV2;
import static io.harness.rule.OwnerRule.BRETT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.zip.Deflater;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EncodingUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testCompression() throws Exception {
    String stringToCompress = generateRandomString();

    byte[] compressedString = compressString(stringToCompress);
    String deCompressedString = deCompressString(compressedString);
    assertThat(deCompressedString).isEqualTo(stringToCompress);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testCompressionWithStringEncoding() throws Exception {
    String stringToCompress = generateRandomString();

    String compressedString = encodeBase64(compressString(stringToCompress));
    String deCompressedString = deCompressString(decodeBase64(compressedString));
    assertThat(deCompressedString).isEqualTo(stringToCompress);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testBase64() throws Exception {
    URL url = this.getClass().getResource("/dos-config.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    assertThat(fileContents).isEqualTo(decodeBase64ToString(encodeBase64(fileContents)));
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testBestDeflaterCompression() throws IOException {
    String stringToCompress = generateRandomString();

    byte[] compressedString = EncodingUtils.compressString(stringToCompress, Deflater.BEST_COMPRESSION);
    String deCompressedString = deCompressString(compressedString);
    assertThat(deCompressedString).isEqualTo(stringToCompress);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testBestDeflaterCompressionWithEncoding() throws IOException {
    String stringToCompress = generateRandomString();

    String compressedString = encodeBase64(EncodingUtils.compressString(stringToCompress, Deflater.BEST_COMPRESSION));
    String deCompressedString = deCompressString(decodeBase64(compressedString));
    assertThat(deCompressedString).isEqualTo(stringToCompress);
  }

  private String generateRandomString() {
    StringBuilder randomStringBuilder = new StringBuilder(generateUuid());
    for (int i = 0; i < 100; i++) {
      randomStringBuilder.append(' ').append(generateUuid());
    }
    return randomStringBuilder.toString();
  }
}
