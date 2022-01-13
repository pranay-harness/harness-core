/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
