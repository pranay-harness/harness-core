/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.stream;

import static io.harness.rule.OwnerRule.AADITI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
