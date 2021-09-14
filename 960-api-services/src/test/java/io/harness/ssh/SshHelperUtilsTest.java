/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ssh;

import static io.harness.rule.OwnerRule.TMACARI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import io.harness.ApiServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;
import io.harness.shell.SshSessionFactory;

import java.io.IOException;
import java.io.Writer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.powermock.core.classloader.annotations.PrepareForTest;

@Slf4j
@PrepareForTest(SshSessionFactory.class)
public class SshHelperUtilsTest extends ApiServiceTestBase {
  private LogCallback logCallback = mock(LogCallback.class);
  private Writer writer = mock(Writer.class);

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteLocalCommand() throws IOException {
    SshHelperUtils.executeLocalCommand("echo test", logCallback, writer, false);
    verify(logCallback, times(1)).saveExecutionLog("test", LogLevel.INFO);
    verifyZeroInteractions(writer);

    SshHelperUtils.executeLocalCommand("echo test", logCallback, writer, true);
    verify(writer, times(1)).write("test");
    verifyZeroInteractions(logCallback);

    SshHelperUtils.executeLocalCommand("echo test >&2", logCallback, writer, false);
    verify(logCallback, times(1)).saveExecutionLog("test", LogLevel.ERROR);
    verifyZeroInteractions(writer);
  }
}
