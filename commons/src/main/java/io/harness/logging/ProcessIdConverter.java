package io.harness.logging;

import com.google.common.base.Splitter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

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
