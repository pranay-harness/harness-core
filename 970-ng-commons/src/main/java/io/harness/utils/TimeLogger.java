/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import org.slf4j.Logger;

@OwnedBy(PL)
public class TimeLogger implements AutoCloseable {
  long startTime;
  Logger logger;

  public TimeLogger(Logger logger) {
    this.startTime = System.currentTimeMillis();
    this.logger = logger;
  }

  @Override
  public void close() {
    long finishTime = System.currentTimeMillis();
    this.logger.info("Time taken {} ms", finishTime - startTime);
  }
}
