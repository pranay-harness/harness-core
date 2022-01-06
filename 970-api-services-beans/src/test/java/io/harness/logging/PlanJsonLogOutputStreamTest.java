/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.logging;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.filesystem.FileIo;
import io.harness.rule.Owner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class PlanJsonLogOutputStreamTest extends CategoryTest {
  private static final String EXAMPLE_PLAN = "{\"created\": [ {\"name\": \"test\"}]}";

  private final HashSet<String> createdFiles = new HashSet<>();
  private PlanJsonLogOutputStream saveLocalJsonLogOutputStream;

  @Before
  public void setup() throws IOException {
    saveLocalJsonLogOutputStream = new PlanJsonLogOutputStream(true);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCompressedWrite() throws IOException {
    try (Writer writer = new OutputStreamWriter(saveLocalJsonLogOutputStream)) {
      writer.write(EXAMPLE_PLAN);
    }

    createdFiles.add(saveLocalJsonLogOutputStream.getTfPlanJsonLocalPath());

    try (GZIPInputStream gzipIs =
             new GZIPInputStream(new FileInputStream(saveLocalJsonLogOutputStream.getTfPlanJsonLocalPath()))) {
      String result = IOUtils.toString(gzipIs, Charset.defaultCharset());
      assertThat(result).isEqualTo(EXAMPLE_PLAN);
    }
  }

  @After
  public void cleanup() {
    for (String file : createdFiles) {
      try {
        FileIo.deleteFileIfExists(file);
      } catch (IOException e) {
        log.error("Failed to delete file: {}", file, e);
      }
    }
  }
}
