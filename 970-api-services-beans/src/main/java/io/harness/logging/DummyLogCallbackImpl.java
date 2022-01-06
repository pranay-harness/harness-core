/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.logging;

import lombok.extern.log4j.Log4j;

@Log4j
public class DummyLogCallbackImpl implements LogCallback {
  @Override
  public void saveExecutionLog(String line) {
    log.info(line);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel) {
    log.info(line);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    log.info(line);
  }
}
