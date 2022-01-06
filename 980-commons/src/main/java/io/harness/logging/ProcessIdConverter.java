/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Splitter;
import java.lang.management.ManagementFactory;

public class ProcessIdConverter extends ClassicConverter {
  private static final String PROCESS_ID =
      Splitter.on("@").split(ManagementFactory.getRuntimeMXBean().getName()).iterator().next();

  @Override
  public String convert(final ILoggingEvent event) {
    // for every logging event return processId from mx bean
    return PROCESS_ID;
  }
}
