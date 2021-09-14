/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import io.harness.version.VersionInfoManager;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class VersionConverter extends ClassicConverter {
  static VersionInfoManager versionInfoManager = new VersionInfoManager();

  @Override
  public String convert(final ILoggingEvent event) {
    return versionInfoManager.getFullVersion();
  }
}
