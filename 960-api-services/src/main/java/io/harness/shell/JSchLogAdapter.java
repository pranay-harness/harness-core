/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.jcraft.jsch.JSch;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class JSchLogAdapter implements com.jcraft.jsch.Logger {
  private int level;
  private JSchLogAdapter() {
    level = toJSchLogLevelOrDefault();
  }

  private static final Map<Integer, Consumer<String>> sl4jLogConsumers = new HashMap<>();
  static {
    sl4jLogConsumers.put(com.jcraft.jsch.Logger.DEBUG, log::debug);
    sl4jLogConsumers.put(com.jcraft.jsch.Logger.INFO, log::info);
    sl4jLogConsumers.put(com.jcraft.jsch.Logger.WARN, log::warn);
    sl4jLogConsumers.put(com.jcraft.jsch.Logger.FATAL, log::error);
    sl4jLogConsumers.put(com.jcraft.jsch.Logger.ERROR, log::error);
  }

  public static JSchLogAdapter attachLogger() {
    JSchLogAdapter jSchLogAdapter = new JSchLogAdapter();
    JSch.setLogger(jSchLogAdapter);
    return jSchLogAdapter;
  }

  public static void detachLogger() {
    JSch.setLogger(null);
  }

  private int toJSchLogLevelOrDefault() {
    if (log.isDebugEnabled() || log.isTraceEnabled()) {
      return com.jcraft.jsch.Logger.DEBUG;
    } else if (log.isInfoEnabled()) {
      return com.jcraft.jsch.Logger.INFO;
    } else if (log.isWarnEnabled()) {
      return com.jcraft.jsch.Logger.WARN;
    } else if (log.isErrorEnabled()) {
      return com.jcraft.jsch.Logger.ERROR;
    }

    return getDefaultLogLevel();
  }

  private int getDefaultLogLevel() {
    return com.jcraft.jsch.Logger.INFO;
  }

  public JSchLogAdapter enableDebugLogLevel() {
    this.level = com.jcraft.jsch.Logger.DEBUG;
    return this;
  }

  public JSchLogAdapter enableInfoLogLevel() {
    this.level = com.jcraft.jsch.Logger.INFO;
    return this;
  }

  @Override
  public boolean isEnabled(int level) {
    return level >= this.level;
  }

  @Override
  public void log(int level, String msg) {
    sl4jLogConsumers.get(level).accept(msg);
  }
}
